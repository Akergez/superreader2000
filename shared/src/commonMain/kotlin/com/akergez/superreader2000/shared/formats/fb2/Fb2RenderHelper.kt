package com.akergez.superreader2000.shared.formats.fb2

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.IRenderHelper
import com.akergez.superreader2000.shared.formats.pdf.TOCEntry
import com.akergez.superreader2000.shared.ui.DrawCommand
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.w3c.dom.Node
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.encoding.Base64
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

data class RenderConfig(
    val pageWidth: Int,
    val pageHeight: Int,
    /** Реальный размер canvas в режиме чтения (= componentWidth/Height из Fb2ReaderScreen). */
    val canvasWidth: Int = pageWidth,
    val canvasHeight: Int = pageHeight,
    val fontSize: TextUnit = 18.sp,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val lineHeightMultiplier: Float = 1.4f,
    /** Горизонтальный отступ страницы в пикселях (density-aware, задаётся из Fb2ReaderScreen). */
    val paddingH: Float = 32f,
    /** Плотность экрана (LocalDensity.current.density), используется для перевода px → sp в Placeholder. */
    val density: Float = 1f,
) {
    // Конфиг валиден только если оба размера положительные
    val isValid: Boolean get() = pageWidth > 0 && pageHeight > 0
}

class Fb2RenderHelper constructor(
    private val renderConfig: MutableStateFlow<RenderConfig>,
    private val textMeasurer: TextMeasurer,
    private val fb2File: FileWrapper,
) : IRenderHelper<Fb2BookContent> {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val imageMap = mutableMapOf<String, ImageBitmap>()
    private val pageCache = ConcurrentHashMap<Int, MutableStateFlow<Fb2BookContent>>()
    // Границы страниц в fullText. Все записи выполняются под paginateMutex, поэтому
    // CopyOnWriteArrayList здесь не нужен: он копировал весь массив при каждой странице.
    private val pageCharOffsets = mutableListOf<Int>()
    private val pageEndCharOffsets = mutableListOf<Int>()

    // Весь текст книги в одной AnnotatedString — строится один раз после парсинга.
    private var fullText = AnnotatedString("")
    // Число word-токенов в fullText[0 until charOffset]. Строится один раз.
    private var wordPrefixAtChar = IntArray(1)
    // Смещение в fullText — граница следующей страницы для пагинации.
    private var charOffset = 0
    private var lastSlicedPageIndex = 0
    private var isAllPageSliced = false

    private val lockParse = Any()
    private val pageBoundaryLock = Any()
    private val paginateMutex = Mutex()
    private var paginationJob: Job? = null
    private var paginatedConfig: RenderConfig? = null
    private var paginationStartedAt: TimeMark? = null
    private var paginationMeasureTime: Duration = Duration.ZERO
    private var paginationMeasureCalls = 0
    private var nextPaginationLogPage = PAGINATION_LOG_INTERVAL
    @Volatile
    private var lastRequestedPageIndex: Int = -1

    val totalWords = MutableStateFlow<Int?>(null)
    val pagesCount = MutableStateFlow<Int?>(null)
    val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())
    val pageWordSum = mutableListOf<Int>()

    // Исходные word-offsets для каждой записи TOC (элементов в списке ≈ слов).
    // Сохраняются один раз при парсинге и используются для перевычисления
    // номеров страниц после каждой перепагинации.
    private val tocWordOffsets = mutableListOf<Int>()

    init {
        scope.launch {
            val elementCount: Int
            val coldOpenMark = TimeSource.Monotonic.markNow()
            var buildTextTime = Duration.ZERO
            var buildWordIndexTime = Duration.ZERO
            lateinit var parseTimings: ParseTimings
            synchronized(lockParse) {
                val result = parseFb2Content(fb2File.file)
                parseTimings = result.timings
                imageMap.putAll(result.images)
                tableOfContents.update { result.toc }
                // Запоминаем исходные word-offsets для переконвертации после пагинации.
                tocWordOffsets.addAll(result.toc.map { it.pageIndex })
                val buildTextMark = TimeSource.Monotonic.markNow()
                fullText = buildFullText(result.elements)
                buildTextTime = buildTextMark.elapsedNow()
                elementCount = result.elements.size
                val wordIndexMark = TimeSource.Monotonic.markNow()
                wordPrefixAtChar = buildWordPrefix(fullText.text)
                buildWordIndexTime = wordIndexMark.elapsedNow()
                totalWords.value = wordPrefixAtChar.last()
            }
            println(
                ">>> fb2 cold parse: xml=${parseTimings.xml.inWholeMilliseconds}ms " +
                    "text=${parseTimings.text.inWholeMilliseconds}ms " +
                    "images=${parseTimings.images.inWholeMilliseconds}ms " +
                    "toc=${parseTimings.toc.inWholeMilliseconds}ms " +
                    "annotated=${buildTextTime.inWholeMilliseconds}ms " +
                    "wordIndex=${buildWordIndexTime.inWholeMilliseconds}ms " +
                    "total=${coldOpenMark.elapsedNow().inWholeMilliseconds}ms " +
                    "elements=$elementCount chars=${fullText.length} " +
                    "spanStyles=${fullText.spanStyles.size} paragraphStyles=${fullText.paragraphStyles.size} " +
                    "images=${imageMap.size}"
            )

            // Явно запускаем пагинацию если конфиг уже валиден на момент старта.
            // StateFlow реплицирует текущее значение при подписке — поэтому используем
            // drop(1) ниже, чтобы collectLatest не вызвал triggerRepagination повторно.
            if (renderConfig.value.isValid) {
                triggerRepagination()
            }

            // Выполняем запросы страниц, поступившие до завершения парсинга.
            pageCache.keys.maxOrNull()?.let { requestedPage ->
                scope.launch { ensurePageAvailable(requestedPage) }
            }

            // Следим за ИЗМЕНЕНИЯМИ конфига (шрифт, ориентация).
            // drop(1) пропускает текущее значение StateFlow — начальный запуск уже выше.
            // collectLatest отменяет пагинацию при новом валидном конфиге.
            renderConfig
                .filter { it.isValid }
                .drop(1)
                .collectLatest {
                    delay(DEBOUNCE_MS)
                    if (renderConfig.value.isValid && renderConfig.value != paginatedConfig) {
                        triggerRepagination()
                    }
                }
        }
    }

    override fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<Fb2BookContent> {
        lastRequestedPageIndex = pageIndex
        // Всегда возвращаем существующий flow — даже если drawCommands пусты.
        // Замена flow в pageCache оставит UI наблюдать за устаревшим объектом.
        pageCache[pageIndex]?.let { flow ->
            scope.launch { ensurePageAvailable(pageIndex) }
            return flow
        }

        val placeholder = MutableStateFlow(
            Fb2BookContent(
                drawCommands = emptyList(),
                imageMap = imageMap,
                pageIndex = pageIndex,
                totalPages = pagesCount.value,
            )
        )
        val flow = pageCache.putIfAbsent(pageIndex, placeholder) ?: placeholder

        scope.launch { ensurePageAvailable(pageIndex) }

        return flow
    }

    override fun close() {
        paginationJob?.cancel()
        pageCache.clear()
        scope.cancel()
    }

    override fun getBookType(): FormatType = FormatType.FB2

    override fun getTableOfContents(): StateFlow<List<TOCEntry>> = tableOfContents

    suspend fun getPageIndexByWordOffset(offset: Int): Int {
        while (true) {
            findPageByWordOffset(offset)?.let { return it }
            if (isAllPageSliced) {
                return synchronized(pageBoundaryLock) { pageWordSum.lastIndex.coerceAtLeast(0) }
            }

            // Пагинируем блоками, а не по одной странице: меньше захватов mutex,
            // пересчётов pagesCount/TOC и переключений корутин.
            val nextTarget = (lastSlicedPageIndex + PAGINATION_BATCH_SIZE).coerceAtLeast(1)
            generatePagesUpTo(nextTarget)
        }
    }

    /** Сбрасывает границы страниц сразу, не дожидаясь debounce RenderConfig. */
    suspend fun repaginate() {
        triggerRepagination()
    }

    /** Word-offset начала страницы, пригодный для восстановления после reflow. */
    fun getPageStartWordOffset(pageIndex: Int): Int? = synchronized(pageBoundaryLock) {
        when {
            pageIndex < 0 -> null
            pageIndex == 0 -> 0
            pageIndex - 1 in pageWordSum.indices -> pageWordSum[pageIndex - 1]
            else -> null
        }
    }

    suspend fun getPageStartWordOffsetAfterPagination(pageIndex: Int): Int? {
        if (pageIndex < 0) return null
        generatePagesUpTo(pageIndex)
        return getPageStartWordOffset(pageIndex)
    }

    private fun findPageByWordOffset(offset: Int): Int? {
        synchronized(pageBoundaryLock) {
            if (pageWordSum.isEmpty() || pageWordSum.last() < offset) return null
            var low = 0
            var high = pageWordSum.lastIndex
            while (low < high) {
                val mid = (low + high) ushr 1
                if (pageWordSum[mid] > offset) high = mid else low = mid + 1
            }
            return low
        }
    }

    private suspend fun triggerRepagination() {
        val newConfig = renderConfig.value
        println(">>> triggerRepagination config=$newConfig")
        if (!newConfig.isValid) {
            println(">>> triggerRepagination SKIPPED - config invalid")
            return
        }

        paginationJob?.cancel()
        paginateMutex.withLock {
            pageCache.values.forEach { flow ->
                flow.update { it.copy(drawCommands = emptyList(), totalPages = null) }
            }
            charOffset = 0
            lastSlicedPageIndex = 0
            isAllPageSliced = false
            paginatedConfig = newConfig
            paginationStartedAt = TimeSource.Monotonic.markNow()
            paginationMeasureTime = Duration.ZERO
            paginationMeasureCalls = 0
            nextPaginationLogPage = PAGINATION_LOG_INTERVAL
            synchronized(pageBoundaryLock) {
                pageWordSum.clear()
                pageCharOffsets.clear()
                pageEndCharOffsets.clear()
            }
        }

        // После смены геометрии повторно публикуем последнюю запрошенную страницу
        // в уже выданный UI поток.
        val requestedPage = lastRequestedPageIndex
        paginationJob = scope.launch {
            generatePagesUpTo(
                if (requestedPage >= 0) {
                    maxOf(3, requestedPage + PAGE_PREFETCH_DISTANCE)
                } else {
                    3
                }
            )
            if (requestedPage >= 0) materializePage(requestedPage)
        }
    }

    private suspend fun ensurePageAvailable(pageIndex: Int) {
        generatePagesUpTo(pageIndex + PAGE_PREFETCH_DISTANCE)
        materializePage(pageIndex)
    }

    private suspend fun generatePagesUpTo(targetPageIndex: Int) {
        paginateMutex.withLock {
            // До завершения parse fullText пуст по определению. Это не пустая книга:
            // нельзя выставлять isAllPageSliced/pagesCount=0 из раннего UI-запроса.
            if (totalWords.value == null) return
            if (isAllPageSliced) return
            if (lastSlicedPageIndex > targetPageIndex) return

            val config = renderConfig.value
            if (!config.isValid) return

            val style = TextStyle(
                fontSize   = config.fontSize,
                fontFamily = config.fontFamily,
                lineHeight = config.fontSize * config.lineHeightMultiplier,
                textAlign  = TextAlign.Justify,
                lineBreak  = LineBreak.Paragraph,
            )
            val w = config.pageWidth.coerceAtLeast(1)
            val h = config.pageHeight.coerceAtLeast(1)

            // Оцениваем размер одной страницы в символах, чтобы не передавать
            // весь оставшийся текст книги в measure() — это было бы O(n²).
            val avgCharWidth = config.fontSize.value * 0.55f
            val charsPerLine = (w / avgCharWidth).toInt().coerceAtLeast(10)
            val linesPerPage = (h / (config.fontSize.value * config.lineHeightMultiplier)).toInt().coerceAtLeast(1)
            val estimatedCharsPerPage = (charsPerLine * linesPerPage).coerceAtLeast(400)
            val baseChunkSize = (estimatedCharsPerPage * LAYOUT_BATCH_PAGES.toLong() *
                CHUNK_SIZE_NUMERATOR / CHUNK_SIZE_DENOMINATOR)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            var nextChunkSize = baseChunkSize
            val batchLayoutHeight = (h.toLong() * LAYOUT_BATCH_PAGES)
                .coerceAtMost(Constraints.Infinity.toLong())
                .toInt()

            while (charOffset < fullText.length) {
                val chunkStartChar = charOffset
                yield()

                var chunkSize = nextChunkSize
                var chunkEnd: Int
                var chunk: AnnotatedString
                var layout: androidx.compose.ui.text.TextLayoutResult
                while (true) {
                    chunkEnd = alignChunkEndToInlineContent(
                        (charOffset + chunkSize).coerceAtMost(fullText.length)
                    )
                    chunk = fullText.subSequence(charOffset, chunkEnd)
                    val measureMark = TimeSource.Monotonic.markNow()
                    layout = textMeasurer.measure(
                        text        = chunk,
                        style       = style,
                        constraints = Constraints(
                            minWidth = w,
                            maxWidth = w,
                            minHeight = 0,
                            maxHeight = batchLayoutHeight,
                        ),
                        softWrap    = true,
                        placeholders = buildChunkPlaceholders(chunk, config),
                    )
                    paginationMeasureTime += measureMark.elapsedNow()
                    paginationMeasureCalls++
                    if (layout.lineCount > 1 || chunkEnd == fullText.length) break
                    chunkSize *= 2
                }

                var firstLineOnPage = 0
                var line = 0
                var committedFromChunk = 0
                while (line < layout.lineCount) {
                    val pageTop = layout.getLineTop(firstLineOnPage)
                    val lineBottom = layout.getLineBottom(line)
                    val overflowsPage = line > firstLineOnPage && lineBottom - pageTop > h
                    if (!overflowsPage) {
                        line++
                        continue
                    }

                    val proposedEnd = chunkStartChar + layout.getLineEnd(line - 1)
                    if (proposedEnd <= charOffset) {
                        // Строка уже покрыта границей, расширенной до конца
                        // атомарного inline-элемента, либо не содержит символов.
                        firstLineOnPage = line
                        line++
                        continue
                    }
                    val inlineAlignedEnd = alignPageEndToInlineContent(
                        pageStart = charOffset,
                        proposedEnd = proposedEnd,
                    )
                    val pageEndChar = alignPageEndToWordBoundary(
                        pageStart = charOffset,
                        proposedEnd = inlineAlignedEnd,
                    )
                    if (pageEndChar <= charOffset) {
                        firstLineOnPage = line
                        line++
                        continue
                    }
                    commitPageBoundary(lastSlicedPageIndex, charOffset, pageEndChar)
                    charOffset = pageEndChar
                    lastSlicedPageIndex++
                    committedFromChunk++
                    reportPaginationProgress()
                    if (committedFromChunk % CANCELLATION_CHECK_PAGE_INTERVAL == 0) yield()

                    // alignPageEndToInlineContent может расширить страницу дальше
                    // lineEnd. Пропускаем все layout-строки, уже вошедшие в диапазон.
                    while (line < layout.lineCount &&
                        chunkStartChar + layout.getLineEnd(line) <= charOffset
                    ) {
                        line++
                    }
                    firstLineOnPage = line

                    if (lastSlicedPageIndex > targetPageIndex) {
                        updateEstimatedPageCount()
                        updateTocPageIndices()
                        return
                    }

                    // Граница следующей страницы должна рассчитываться layout'ом,
                    // который начинается ровно с этой границы. Продолжать текущий
                    // многостраничный layout нельзя: viewer измеряет страницы отдельно.
                    break
                }

                if (committedFromChunk > 0) {
                    nextChunkSize = baseChunkSize
                    continue
                }

                // maxHeight может обрезать layout даже тогда, когда chunk уже дошёл
                // до конца fullText. Завершать книгу можно только если весь chunk
                // действительно был разложен, иначе остаток ошибочно станет одной страницей.
                if (chunkEnd == fullText.length && !layout.didOverflowHeight) {
                    if (charOffset < fullText.length) {
                        commitPageBoundary(lastSlicedPageIndex, charOffset, fullText.length)
                        charOffset = fullText.length
                        lastSlicedPageIndex++
                    }
                    isAllPageSliced = true
                    pagesCount.update { lastSlicedPageIndex }
                    updateTocPageIndices()
                    reportPaginationProgress(force = true)
                    return
                }

                // Последняя группа строк в chunk неполная. Она не фиксируется и будет
                // повторно измерена как начало следующего блока.
                nextChunkSize = if (committedFromChunk == 0) {
                    (chunkSize.toLong() * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                } else {
                    baseChunkSize
                }
            }

            isAllPageSliced = true
            pagesCount.update { lastSlicedPageIndex }
            updateTocPageIndices()
            reportPaginationProgress(force = true)
        }
    }

    private fun reportPaginationProgress(force: Boolean = false) {
        if (!force && lastSlicedPageIndex < nextPaginationLogPage) return
        val elapsed = paginationStartedAt?.elapsedNow() ?: Duration.ZERO
        println(
            ">>> fb2 paginate: pages=$lastSlicedPageIndex chars=$charOffset " +
                "measureCalls=$paginationMeasureCalls " +
                "measure=${paginationMeasureTime.inWholeMilliseconds}ms " +
                "elapsed=${elapsed.inWholeMilliseconds}ms complete=$isAllPageSliced"
        )
        while (nextPaginationLogPage <= lastSlicedPageIndex) {
            nextPaginationLogPage += PAGINATION_LOG_INTERVAL
        }
    }

    private fun updateEstimatedPageCount() {
        val charsPerPage = (charOffset / lastSlicedPageIndex).coerceAtLeast(1)
        val estimated = (fullText.length / charsPerPage).coerceAtLeast(lastSlicedPageIndex)
        pagesCount.update { estimated }
    }

    /**
     * Переводит word-offsets записей TOC в реальные номера страниц.
     * Вызывается каждый раз, когда пагинация завершена (isAllPageSliced = true).
     */
    private fun updateTocPageIndices() {
        if (tocWordOffsets.isEmpty() || pageWordSum.isEmpty()) return
        val maxPaginatedWords = pageWordSum.last()
        val toc = tableOfContents.value
        var anyChanged = false
        val updated = toc.mapIndexed { i, entry ->
            val wordOffset = tocWordOffsets.getOrElse(i) { 0 }
            if (wordOffset > maxPaginatedWords) {
                // Эта глава ещё не отпагинирована — оставляем как есть.
                entry
            } else {
                val pageIdx = findPageByWordOffset(wordOffset) ?: return@mapIndexed entry
                if (pageIdx != entry.pageIndex) anyChanged = true
                entry.copy(pageIndex = pageIdx)
            }
        }
        if (anyChanged) tableOfContents.update { updated }
    }

    /** Строит DrawCommand из уже измеренного layout — один блок на всю страницу.
     *  [lastLine] — последняя строка для отрисовки (включительно). */
    private fun buildDrawCommandsFromLayout(
        layout: androidx.compose.ui.text.TextLayoutResult,
        pageText: AnnotatedString,
        config: RenderConfig,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
    ): List<DrawCommand> {
        if (pageText.text.isBlank() && pageText.getStringAnnotations(INLINE_CONTENT_TAG, 0, pageText.length).isEmpty()) return emptyList()
        val commands = mutableListOf<DrawCommand>()
        commands.add(
            DrawCommand.DrawText(
                text          = pageText,
                x             = config.paddingH,
                y             = PADDING_V,
                style         = TextStyle(
                    fontSize   = config.fontSize,
                    fontFamily = config.fontFamily,
                    lineHeight = config.fontSize * config.lineHeightMultiplier,
                    textAlign  = TextAlign.Justify,
                    lineBreak  = LineBreak.Paragraph,
                ),
                textAreaWidth = config.pageWidth.toFloat(),
                placeholders  = placeholders,
            )
        )
        // Emit DrawInlineImage for each image placeholder that falls within this page.
        // placeholderRects is indexed in the same order as the placeholders passed to measure(),
        // which matches the order images appear in the chunk. Page images are a prefix of chunk images.
        val pageImages = pageText
            .getStringAnnotations(INLINE_CONTENT_TAG, 0, pageText.length)
            .distinctBy { Triple(it.start, it.end, it.item) }
        pageImages.forEachIndexed { idx, ann ->
            val rect = layout.placeholderRects.getOrNull(idx) ?: return@forEachIndexed
            if (!imageMap.containsKey(ann.item)) return@forEachIndexed
            commands.add(
                DrawCommand.DrawInlineImage(
                    imageId = ann.item,
                    x       = config.paddingH + rect.left,
                    y       = PADDING_V + rect.top,
                    width   = rect.width.coerceAtLeast(1f),
                    height  = rect.height.coerceAtLeast(1f),
                )
            )
        }
        return commands
    }

    /** Builds the placeholders list for [TextMeasurer.measure] from image annotations in [chunk]. */
    private fun buildChunkPlaceholders(
        chunk: AnnotatedString,
        config: RenderConfig,
    ): List<AnnotatedString.Range<Placeholder>> {
        val imageAnnotations = chunk
            .getStringAnnotations(INLINE_CONTENT_TAG, 0, chunk.length)
            .distinctBy { Triple(it.start, it.end, it.item) }
        if (imageAnnotations.isEmpty()) return emptyList()
        val density = config.density.coerceAtLeast(1f)
        val maxWidthPx = config.pageWidth.toFloat()
        return imageAnnotations.mapNotNull { ann ->
            val bitmap = imageMap[ann.item] ?: return@mapNotNull null
            val sourceWidth = bitmap.width.coerceAtLeast(1).toFloat()
            val sourceHeight = bitmap.height.coerceAtLeast(1).toFloat()
            val maxHeightPx = config.pageHeight * 0.85f
            // Один коэффициент для обеих осей: изображение целиком помещается
            // в страницу и никогда не меняет исходное соотношение сторон.
            val scale = minOf(maxWidthPx / sourceWidth, maxHeightPx / sourceHeight)
            val imgWidthPx = sourceWidth * scale
            val imgHeightPx = sourceHeight * scale
            AnnotatedString.Range(
                item  = Placeholder(
                    width                   = (imgWidthPx / density).sp,
                    height                  = (imgHeightPx / density).sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
                start = ann.start,
                end   = ann.end,
            )
        }
    }

    /** Не позволяет рабочему chunk закончиться внутри alternateText inline-image. */
    private fun alignChunkEndToInlineContent(proposedEnd: Int): Int {
        if (proposedEnd <= 0 || proposedEnd >= fullText.length) return proposedEnd
        val crossing = fullText
            .getStringAnnotations(INLINE_CONTENT_TAG, proposedEnd - 1, proposedEnd + 1)
            .firstOrNull { it.start < proposedEnd && proposedEnd < it.end }
        return crossing?.end ?: proposedEnd
    }

    /**
     * Inline-image является атомарным элементом. Если line boundary всё же попал
     * внутрь его alternateText, переносим всё изображение на следующую страницу.
     */
    private fun alignPageEndToInlineContent(pageStart: Int, proposedEnd: Int): Int {
        if (proposedEnd <= pageStart || proposedEnd >= fullText.length) return proposedEnd
        val crossing = fullText
            .getStringAnnotations(INLINE_CONTENT_TAG, proposedEnd - 1, proposedEnd + 1)
            .firstOrNull { it.start < proposedEnd && proposedEnd < it.end }
            ?: return proposedEnd
        // Страница не должна состоять только из разделителей перед inline-элементом.
        val contentBeforeImage = fullText.text.substring(pageStart, crossing.start)
        return if (crossing.start > pageStart && contentBeforeImage.isNotBlank()) {
            crossing.start
        } else {
            crossing.end
        }
    }

    /** Не оставляет начало/окончание одного слова на разных страницах. */
    private fun alignPageEndToWordBoundary(pageStart: Int, proposedEnd: Int): Int {
        if (proposedEnd <= pageStart || proposedEnd >= fullText.length) return proposedEnd
        val text = fullText.text
        if (!text[proposedEnd - 1].isWordPart() || !text[proposedEnd].isWordPart()) {
            return proposedEnd
        }

        var wordStart = proposedEnd
        while (wordStart > pageStart && text[wordStart - 1].isWordPart()) {
            wordStart--
        }
        // Очень длинное слово не должно образовать пустую страницу.
        return if (wordStart > pageStart) wordStart else proposedEnd
    }

    private fun Char.isWordPart(): Boolean =
        isLetterOrDigit() || this == '\'' || this == '’' || this == '-' ||
            this == '‐' || this == '‑' || this == '\u00ad'

    private fun commitPageBoundary(index: Int, pageStartChar: Int, pageEndChar: Int) {
        val expectedStart = synchronized(pageBoundaryLock) {
            pageEndCharOffsets.lastOrNull() ?: 0
        }
        check(index == synchronized(pageBoundaryLock) { pageEndCharOffsets.size }) {
            "Non-sequential FB2 page index: expected ${pageEndCharOffsets.size}, got $index"
        }
        check(pageStartChar == expectedStart) {
            "Gap or overlap between FB2 pages: expected start $expectedStart, got $pageStartChar"
        }
        check(pageEndChar > pageStartChar) {
            "Empty FB2 page range: $pageStartChar..$pageEndChar"
        }
        if (pageEndChar < fullText.length) {
            val splitsInlineContent = fullText
                .getStringAnnotations(INLINE_CONTENT_TAG, pageEndChar - 1, pageEndChar + 1)
                .any { it.start < pageEndChar && pageEndChar < it.end }
            check(!splitsInlineContent) {
                "FB2 page boundary splits inline content at $pageEndChar"
            }
        }
        synchronized(pageBoundaryLock) {
            pageWordSum.add(wordPrefixAtChar[pageEndChar])
            pageCharOffsets.add(pageStartChar)
            pageEndCharOffsets.add(pageEndChar)
        }
    }

    private fun buildWordPrefix(text: String): IntArray {
        val result = IntArray(text.length + 1)
        var completedWords = 0
        var tokenHasContent = false
        for (i in text.indices) {
            if (text[i] == ' ') {
                if (tokenHasContent) completedWords++
                tokenHasContent = false
            } else if (!text[i].isWhitespace()) {
                tokenHasContent = true
            }
            result[i + 1] = completedWords + if (tokenHasContent) 1 else 0
        }
        return result
    }

    private fun publishPage(
        index: Int,
        pageText: AnnotatedString,
        layout: androidx.compose.ui.text.TextLayoutResult,
        config: RenderConfig,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
    ) {
        val commands = buildDrawCommandsFromLayout(layout, pageText, config, placeholders)

        val content = Fb2BookContent(
            drawCommands = commands,
            imageMap     = imageMap,
            pageIndex    = index,
            totalPages   = pagesCount.value,
            nativeWidth  = config.canvasWidth,
            nativeHeight = config.canvasHeight,
        )
        pageCache[index]?.update { content }
    }

    private suspend fun materializePage(index: Int) {
        paginateMutex.withLock {
            val flow = pageCache[index] ?: return
            if (flow.value.drawCommands.isNotEmpty()) return
            if (index !in pageCharOffsets.indices || index !in pageEndCharOffsets.indices) return

            val config = renderConfig.value
            if (!config.isValid) return
            val pageText = fullText.subSequence(pageCharOffsets[index], pageEndCharOffsets[index])
            val style = TextStyle(
                fontSize = config.fontSize,
                fontFamily = config.fontFamily,
                lineHeight = config.fontSize * config.lineHeightMultiplier,
                textAlign = TextAlign.Justify,
                lineBreak = LineBreak.Paragraph,
            )
            val placeholders = buildChunkPlaceholders(pageText, config)
            val materializeMark = TimeSource.Monotonic.markNow()
            val layout = textMeasurer.measure(
                text = pageText,
                style = style,
                constraints = Constraints(
                    minWidth = config.pageWidth,
                    maxWidth = config.pageWidth,
                    minHeight = 0,
                    maxHeight = config.pageHeight,
                ),
                softWrap = true,
                placeholders = placeholders,
            )
            val materializeTime = materializeMark.elapsedNow()
            publishPage(index, pageText, layout, config, placeholders)
            println(
                ">>> fb2 materialize: page=$index chars=${pageText.length} " +
                    "measure=${materializeTime.inWholeMilliseconds}ms"
            )
        }
    }

    /**
     * Мгновенная оценка номера страницы по word-offset через пропорцию в символах.
     * Не требует пагинации — работает как только парсинг завершён.
     * Точность: ±несколько страниц (достаточно для немедленного показа).
     */
    fun estimatePageFromWordOffset(wordOffset: Int): Int {
        val totalW = totalWords.value ?: return 0
        if (totalW <= 0 || fullText.isEmpty()) return 0
        val estimatedChar = (wordOffset.toLong() * fullText.length / totalW)
            .toInt().coerceIn(0, fullText.length)

        // Если уже отпагинированы страницы — бинарный поиск по реальным смещениям.
        val offsets = synchronized(pageBoundaryLock) { pageCharOffsets.toList() }
        if (offsets.size > 1) {
            var lo = 0; var hi = offsets.size - 1
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                if (offsets[mid] <= estimatedChar) lo = mid else hi = mid - 1
            }
            return lo
        }

        // Грубая оценка по метрике шрифта — до появления реальных данных.
        val config = renderConfig.value
        if (!config.isValid) return 0
        val avgCharWidth = config.fontSize.value * 0.55f
        val charsPerLine = (config.pageWidth / avgCharWidth).toInt().coerceAtLeast(1)
        val linesPerPage = (config.pageHeight / (config.fontSize.value * config.lineHeightMultiplier))
            .toInt().coerceAtLeast(1)
        val charsPerPage = (charsPerLine * linesPerPage).coerceAtLeast(1)
        return (estimatedChar / charsPerPage).coerceAtLeast(0)
    }

    /** Собирает всю книгу в одну AnnotatedString (один раз после парсинга). */
    private fun buildFullText(elements: List<FB2Element>): AnnotatedString = buildAnnotatedString {
        // Знаки, перед которыми не нужен пробел (знаки примыкают к предыдущему слову).
        val noPrecedingSpace = setOf('.', ',', '!', '?', ':', ';', ')', ']', '}', '»', '…')
        // Дефис может оказаться отдельным DOM text-node между тегами форматирования.
        // Внутри слова пробел не нужен ни перед ним, ни после него.
        val wordJoiners = setOf('-', '‐', '‑', '\u00ad')
        var previousText: String? = null
        elements.forEach { element ->
            val cur = element.toStyledString()
            previousText?.let { prevText ->
                val curText  = cur.text
                // Не добавляем пробел: перед знаком препинания, перед/после переноса строки,
                // или если предыдущий элемент был пустым.
                val noSpace = curText.isEmpty()
                    || curText[0] in noPrecedingSpace
                    || curText[0] in wordJoiners
                    || prevText.lastOrNull() in wordJoiners
                    || curText[0] == '\n'
                    || prevText.lastOrNull() == '\n'
                    || prevText.isBlank()
                if (!noSpace) append(' ')
            }
            append(cur)
            previousText = cur.text
        }
    }

    private data class ParseTimings(
        val xml: Duration,
        val text: Duration,
        val images: Duration,
        val toc: Duration,
    )

    private data class ParseResult(
        val elements: List<FB2Element>,
        val images: Map<String, ImageBitmap>,
        val toc: List<TOCEntry>,
        val timings: ParseTimings,
    )

    private fun parseFb2Content(fb2File: File): ParseResult {
        return try {
            val xmlMark = TimeSource.Monotonic.markNow()
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(fb2File)
            doc.documentElement.normalize()
            val xmlTime = xmlMark.elapsedNow()

            val tocEntries = mutableListOf<TOCEntry>()
            val elements   = mutableListOf<FB2Element>()
            val images     = mutableMapOf<String, ImageBitmap>()

            val textMark = TimeSource.Monotonic.markNow()
            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name != "notes") {
                    elements.addAll(extractTextContent(body, tocEntries, elements.size))
                }
            }
            val textTime = textMark.elapsedNow()

            val imagesMark = TimeSource.Monotonic.markNow()
            val binaryElements = doc.getElementsByTagName("binary")
            for (i in 0 until binaryElements.length) {
                val node        = binaryElements.item(i)
                val contentType = node.attributes?.getNamedItem("content-type")?.nodeValue
                val id          = node.attributes?.getNamedItem("id")?.nodeValue
                if (contentType?.startsWith("image/") == true && id != null) {
                    val base64Content = node.textContent?.trim() ?: ""
                    if (base64Content.isNotEmpty()) {
                        try {
                            val bytes = Base64.decode(base64Content)
                            images[id] = bytes.decodeToImageBitmap()
                        } catch (_: Exception) { /* skip malformed image */ }
                    }
                }
            }
            val imagesTime = imagesMark.elapsedNow()

            // Convert element-index offsets to actual word-count offsets.
            // Empty/newline elements (FB2Text(""), FB2Text("\n")) count as 1 element but 0 words,
            // so element index != word count. Build a table and remap.
            val tocMark = TimeSource.Monotonic.markNow()
            val elemToWordCount = IntArray(elements.size + 1)
            var wc = 0
            elements.forEachIndexed { i, elem ->
                elemToWordCount[i] = wc
                if (elem.contributesWordOffset()) wc++
            }
            elemToWordCount[elements.size] = wc

            val correctedToc = tocEntries.map { entry ->
                entry.copy(pageIndex = elemToWordCount.getOrElse(entry.pageIndex) { entry.pageIndex })
            }
            val tocTime = tocMark.elapsedNow()

            ParseResult(
                elements = elements,
                images = images,
                toc = correctedToc.distinctBy { it.title },
                timings = ParseTimings(xmlTime, textTime, imagesTime, tocTime),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ParseResult(
                elements = listOf(FB2ColoredText("Can't parse book", Color.Red)),
                images   = emptyMap(),
                toc      = emptyList(),
                timings  = ParseTimings(Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO),
            )
        }
    }

    // outerOffset = number of elements already accumulated in the outer (book-level) list before
    // this call. Used so TOC pageIndex records the absolute element position in the full book,
    // not the position relative to the current recursive call.
    private fun extractTextContent(
        node: Node,
        tocEntries: MutableList<TOCEntry>,
        outerOffset: Int = 0,
    ): List<FB2Element> {
        val elements = mutableListOf<FB2Element>()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            when (child.nodeType) {
                Node.TEXT_NODE -> {
                    val text = child.nodeValue?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        appendTextTokens(text, elements)
                    }
                }
                Node.ELEMENT_NODE -> {
                    val childOffset = outerOffset + elements.size
                    val extracted = extractTextContent(child, tocEntries, childOffset)
                    when (child.nodeName) {
                        "empty-line" -> elements += FB2Text("")
                        "emphasis"   -> extracted.forEach { elements += FB2ItalicText(it) }
                        "strong"     -> extracted.forEach { elements += FB2BoldText(it) }
                        "code"       -> extracted.forEach { elements += FB2CodeText(it) }
                        "title" -> {
                            elements += FB2Text("\n")
                            extracted.forEach { elements += FB2BoldText(it) }
                            elements += FB2Text("\n")
                            if (extracted.isNotEmpty()) {
                                tocEntries += TOCEntry(
                                    title     = child.textContent.trim(),
                                    pageIndex = outerOffset + elements.size,
                                    children  = emptyList(),
                                )
                            }
                        }
                        "subtitle" -> {
                            elements += FB2Text("\n")
                            elements += FB2SubtitleText(
                                combineAnnotatedStrings(extracted.map { it.toStyledString() })
                            )
                            elements += FB2Text("\n")
                        }
                        "p" -> {
                            if (extracted.isNotEmpty()) {
                                elements += FB2Paragraph(extracted[0])
                                for (index in 1 until extracted.size) {
                                    elements += FB2Text(extracted[index])
                                }
                            }
                        }
                        "a" -> {
                            val href = child.attributes?.getNamedItem("href")?.nodeValue
                            if (href != null) {
                                extracted.forEach { elements += FB2ColoredText(it, Color.Blue) }
                            } else {
                                elements.addAll(extracted)
                            }
                        }
                        "image" -> {
                            val href = child.attributes?.getNamedItem("l:href")?.nodeValue
                            val alt = child.attributes?.getNamedItem("alt")?.nodeValue?.trim() ?: ""
                            if (href != null && href.startsWith("#")) {
                                elements += FB2Image(imageId = href.substring(1), description = alt)
                            }
                        }
                        else -> elements.addAll(extracted)
                    }
                }
            }
        }
        return elements
    }

    /** Эквивалент split(' ', '\n', '\r') + trim, но без промежуточных списков. */
    private fun appendTextTokens(text: String, target: MutableList<FB2Element>) {
        var tokenStart = 0
        for (index in 0..text.length) {
            val atEnd = index == text.length
            val isSeparator = !atEnd && (text[index] == ' ' || text[index] == '\n' || text[index] == '\r')
            if (!atEnd && !isSeparator) continue

            if (index > tokenStart) {
                val token = text.substring(tokenStart, index).trim()
                if (token.isNotBlank()) target += FB2Text(token)
            }
            tokenStart = index + 1
        }
    }

    private fun combineAnnotatedStrings(list: List<AnnotatedString>): AnnotatedString =
        buildAnnotatedString { list.forEach { append(it) } }

    companion object {
        private const val PADDING_V          = 32f
        private const val DEBOUNCE_MS        = 300L
        private const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"
        private const val PAGINATION_BATCH_SIZE = 32
        private const val PAGE_PREFETCH_DISTANCE = 2
        private const val CANCELLATION_CHECK_PAGE_INTERVAL = 8
        private const val PAGINATION_LOG_INTERVAL = 250
        // Страница должна измеряться из собственного начального offset: именно так
        // тот же текст затем измеряет Fb2Viewer. Это исключает расхождение границ.
        private const val LAYOUT_BATCH_PAGES = 1
        private const val CHUNK_SIZE_NUMERATOR = 5
        private const val CHUNK_SIZE_DENOMINATOR = 4
    }
}

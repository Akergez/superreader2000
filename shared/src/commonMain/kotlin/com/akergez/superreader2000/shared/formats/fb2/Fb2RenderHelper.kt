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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.encoding.Base64

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
    private val pageCache = mutableMapOf<Int, MutableStateFlow<Fb2BookContent>>()
    // Смещение в fullText (символы) в начале каждой страницы — для быстрой оценки позиции.
    private val pageCharOffsets = java.util.concurrent.CopyOnWriteArrayList<Int>()

    // Весь текст книги в одной AnnotatedString — строится один раз после парсинга.
    private var fullText = AnnotatedString("")
    // Смещение в fullText — граница следующей страницы для пагинации.
    private var charOffset = 0
    private var lastSlicedPageIndex = 0
    private var isAllPageSliced = false

    private val lockParse = Any()
    private val paginateMutex = Mutex()
    private var paginationJob: Job? = null

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
            synchronized(lockParse) {
                val result = parseFb2Content(fb2File.file)
                imageMap.putAll(result.images)
                tableOfContents.update { result.toc }
                // Запоминаем исходные word-offsets для переконвертации после пагинации.
                tocWordOffsets.addAll(result.toc.map { it.pageIndex })
                fullText = buildFullText(result.elements)
                elementCount = result.elements.size
                totalWords.update { elementCount }
            }
            println(">>> parsed $elementCount elements, fullText=${fullText.length} chars, config=${renderConfig.value}")

            // Явно запускаем пагинацию если конфиг уже валиден на момент старта.
            // StateFlow реплицирует текущее значение при подписке — поэтому используем
            // drop(1) ниже, чтобы collectLatest не вызвал triggerRepagination повторно.
            if (renderConfig.value.isValid) {
                triggerRepagination()
            }

            // Следим за ИЗМЕНЕНИЯМИ конфига (шрифт, ориентация).
            // drop(1) пропускает текущее значение StateFlow — начальный запуск уже выше.
            // collectLatest отменяет пагинацию при новом валидном конфиге.
            renderConfig
                .filter { it.isValid }
                .drop(1)
                .collectLatest {
                    delay(DEBOUNCE_MS)
                    if (renderConfig.value.isValid) {
                        triggerRepagination()
                    }
                }
        }
    }

    override fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<Fb2BookContent> {
        // Всегда возвращаем существующий flow — даже если drawCommands пусты.
        // Замена flow в pageCache оставит UI наблюдать за устаревшим объектом.
        pageCache[pageIndex]?.let { flow ->
            scope.launch { generatePagesUpTo(pageIndex + 2) }
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
        pageCache[pageIndex] = placeholder

        scope.launch { generatePagesUpTo(pageIndex + 2) }

        return placeholder
    }

    override fun close() {
        paginationJob?.cancel()
        pageCache.clear()
        scope.cancel()
    }

    override fun getBookType(): FormatType = FormatType.FB2

    override fun getTableOfContents(): StateFlow<List<TOCEntry>> = tableOfContents

    suspend fun getPageIndexByWordOffset(offset: Int): Int {
        var index = 0
        while (!isAllPageSliced) {
            if (pageWordSum.size > index && pageWordSum[index] > offset) return index
            generatePagesUpTo(++index)
        }
        return pageWordSum.indexOfFirst { it >= offset }.coerceAtLeast(0)
    }

    private suspend fun triggerRepagination() {
        println(">>> triggerRepagination config=${renderConfig.value}")
        if (!renderConfig.value.isValid) {
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
            pageWordSum.clear()
            pageCharOffsets.clear()
        }

        paginationJob = scope.launch { generatePagesUpTo(3) }
    }

    private suspend fun generatePagesUpTo(targetPageIndex: Int) {
        paginateMutex.withLock {
            if (isAllPageSliced) return
            if (pageCache[targetPageIndex]?.value?.drawCommands?.isNotEmpty() == true) return

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
            val baseChunkSize = (charsPerLine * linesPerPage * 2).coerceAtLeast(400)

            while (charOffset < fullText.length) {
                val pageStartChar = charOffset  // фиксируем начало страницы до измерения
                yield() // кооперативная отмена — раз на страницу

                // Берём кусок ~2× одной страницы. Если он целиком поместился
                // (маловероятно при правильной оценке), удваиваем и повторяем.
                var chunkSize = baseChunkSize
                var chunkEnd: Int
                var chunk: AnnotatedString
                var layout: androidx.compose.ui.text.TextLayoutResult
                while (true) {
                    chunkEnd = (charOffset + chunkSize).coerceAtMost(fullText.length)
                    chunk = fullText.subSequence(charOffset, chunkEnd)
                    layout = textMeasurer.measure(
                        text        = chunk,
                        style       = style,
                        constraints = Constraints(minWidth = w, maxWidth = w, minHeight = 0, maxHeight = h),
                        softWrap    = true,
                        placeholders = buildChunkPlaceholders(chunk, config),
                    )
                    if (layout.didOverflowHeight || chunkEnd == fullText.length) break
                    chunkSize *= 2
                }

                // Ищем последнюю строку, которая целиком влезает в высоту страницы.
                // layout.lineCount включает ВСЕ строки (и те, что вышли за maxHeight),
                // поэтому нельзя брать layout.lineCount - 1 напрямую.
                val lastFittingLine: Int
                val pageEndInChunk: Int
                if (layout.didOverflowHeight) {
                    var last = 0
                    for (i in 0 until layout.lineCount) {
                        if (layout.getLineBottom(i) > h) break
                        last = i
                    }
                    lastFittingLine = last
                    pageEndInChunk = layout.getLineEnd(last)
                } else {
                    lastFittingLine = layout.lineCount - 1
                    pageEndInChunk = chunk.length
                }

                val commands = buildDrawCommandsFromLayout(layout, chunk, config, lastFittingLine)
                val pageText = chunk.subSequence(0, pageEndInChunk)

                commitPageData(lastSlicedPageIndex, pageText, commands, config, pageStartChar)
                charOffset += pageEndInChunk
                lastSlicedPageIndex++

                if (!layout.didOverflowHeight) {
                    isAllPageSliced = true
                    pagesCount.update { lastSlicedPageIndex }
                    updateTocPageIndices()
                    return
                }

                if (lastSlicedPageIndex > targetPageIndex) {
                    // Пересчитываем оценку каждый раз по реальному соотношению символов/страниц
                    val charsPerPage = (charOffset / lastSlicedPageIndex).coerceAtLeast(1)
                    val estimated = (fullText.length / charsPerPage).coerceAtLeast(lastSlicedPageIndex)
                    pagesCount.update { estimated }
                    // Обновляем TOC для уже отпагинированных глав.
                    updateTocPageIndices()
                    return
                }
            }

            isAllPageSliced = true
            pagesCount.update { lastSlicedPageIndex }
            updateTocPageIndices()
        }
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
                val pageIdx = pageWordSum.indexOfFirst { it >= wordOffset }.coerceAtLeast(0)
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
        text: AnnotatedString,
        config: RenderConfig,
        lastLine: Int = layout.lineCount - 1,
    ): List<DrawCommand> {
        val pageEnd = layout.getLineEnd(lastLine).coerceAtMost(text.length)
        val pageText = text.subSequence(0, pageEnd)
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
                placeholders  = buildChunkPlaceholders(pageText, config),
            )
        )
        // Emit DrawInlineImage for each image placeholder that falls within this page.
        // placeholderRects is indexed in the same order as the placeholders passed to measure(),
        // which matches the order images appear in the chunk. Page images are a prefix of chunk images.
        val pageImages = pageText.getStringAnnotations(INLINE_CONTENT_TAG, 0, pageText.length)
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
        val imageAnnotations = chunk.getStringAnnotations(INLINE_CONTENT_TAG, 0, chunk.length)
        if (imageAnnotations.isEmpty()) return emptyList()
        val density = config.density.coerceAtLeast(1f)
        val maxWidthPx = config.pageWidth.toFloat()
        return imageAnnotations.mapNotNull { ann ->
            val bitmap = imageMap[ann.item] ?: return@mapNotNull null
            val aspectRatio = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height else 1f
            val imgWidthPx = maxWidthPx
            val imgHeightPx = (imgWidthPx / aspectRatio).coerceAtMost(config.pageHeight * 0.85f)
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

    private fun commitPageData(index: Int, pageText: AnnotatedString, commands: List<DrawCommand>, config: RenderConfig, pageStartChar: Int = 0) {
        println(">>> commitPage index=$index commands=${commands.size}")
        val wordsOnPage = pageText.text.split(' ').count { it.isNotBlank() }
        while (pageWordSum.size < index) pageWordSum.add(pageWordSum.lastOrNull() ?: 0)
        pageWordSum.add(wordsOnPage + (pageWordSum.lastOrNull() ?: 0))

        val content = Fb2BookContent(
            drawCommands = commands,
            imageMap     = imageMap,
            pageIndex    = index,
            totalPages   = pagesCount.value,
            nativeWidth  = config.canvasWidth,
            nativeHeight = config.canvasHeight,
        )
        if (pageCache.containsKey(index)) {
            pageCache[index]!!.update { content }
        } else {
            pageCache[index] = MutableStateFlow(content)
        }
        if (pageCharOffsets.size <= index) {
            pageCharOffsets.add(pageStartChar)
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
        val offsets = pageCharOffsets
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
        val styled = elements.map { it.toStyledString() }
        styled.forEachIndexed { i, cur ->
            if (i > 0) {
                val prevText = styled[i - 1].text
                val curText  = cur.text
                // Не добавляем пробел: перед знаком препинания, перед/после переноса строки,
                // или если предыдущий элемент был пустым.
                val noSpace = curText.isEmpty()
                    || curText[0] in noPrecedingSpace
                    || curText[0] == '\n'
                    || prevText.lastOrNull() == '\n'
                    || prevText.isBlank()
                if (!noSpace) append(' ')
            }
            append(cur)
        }
    }

    private data class ParseResult(
        val elements: List<FB2Element>,
        val images: Map<String, ImageBitmap>,
        val toc: List<TOCEntry>,
    )

    private fun parseFb2Content(fb2File: File): ParseResult {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(fb2File)
            doc.documentElement.normalize()

            val tocEntries = mutableListOf<TOCEntry>()
            val elements   = mutableListOf<FB2Element>()
            val images     = mutableMapOf<String, ImageBitmap>()

            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name != "notes") {
                    elements.addAll(extractTextContent(body, tocEntries, elements.size))
                }
            }

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

            // Convert element-index offsets to actual word-count offsets.
            // Empty/newline elements (FB2Text(""), FB2Text("\n")) count as 1 element but 0 words,
            // so element index != word count. Build a table and remap.
            val elemToWordCount = IntArray(elements.size + 1)
            var wc = 0
            elements.forEachIndexed { i, elem ->
                elemToWordCount[i] = wc
                if (elem.toStyledString().text.isNotBlank()) wc++
            }
            elemToWordCount[elements.size] = wc

            val correctedToc = tocEntries.map { entry ->
                entry.copy(pageIndex = elemToWordCount.getOrElse(entry.pageIndex) { entry.pageIndex })
            }

            ParseResult(elements, images, correctedToc.distinctBy { it.title })
        } catch (e: Exception) {
            e.printStackTrace()
            ParseResult(
                elements = listOf(FB2ColoredText("Can't parse book", Color.Red)),
                images   = emptyMap(),
                toc      = emptyList(),
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
                        text.split(' ', '\n', '\r')
                            .filter { it.isNotBlank() }
                            .forEach { elements += FB2Text(it.trim()) }
                    }
                }
                Node.ELEMENT_NODE -> {
                    val childOffset = outerOffset + elements.size
                    val extracted = extractTextContent(child, tocEntries, childOffset)
                    when (child.nodeName) {
                        "empty-line" -> elements += FB2Text("")
                        "emphasis"   -> elements.addAll(extracted.map { FB2ItalicText(it) })
                        "strong"     -> elements.addAll(extracted.map { FB2BoldText(it) })
                        "code"       -> elements.addAll(extracted.map { FB2CodeText(it) })
                        "title" -> {
                            elements += FB2Text("\n")
                            elements.addAll(extracted.map { FB2BoldText(it) })
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
                                elements.addAll(extracted.drop(1).map { FB2Text(it) })
                            }
                        }
                        "a" -> {
                            val href = child.attributes?.getNamedItem("href")?.nodeValue
                            if (href != null) elements.addAll(extracted.map { FB2ColoredText(it, Color.Blue) })
                            else elements.addAll(extracted)
                        }
                        "image" -> {
                            val href = child.attributes?.getNamedItem("l:href")?.nodeValue
                            val alt = child.attributes?.getNamedItem("alt")?.nodeValue?.trim() ?: ""
                            if (href != null && href.startsWith("#")) {
                                elements += FB2Image(imageId = href.substring(1), description = alt)
                            }
                        }
                        else -> elements.addAll(extractTextContent(child, tocEntries, childOffset))
                    }
                }
            }
        }
        return elements
    }

    private fun combineAnnotatedStrings(list: List<AnnotatedString>): AnnotatedString =
        buildAnnotatedString { list.forEach { append(it) } }

    companion object {
        private const val PADDING_H          = 32f
        private const val PADDING_V          = 32f
        private const val DEBOUNCE_MS        = 300L
        private const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"
    }
}
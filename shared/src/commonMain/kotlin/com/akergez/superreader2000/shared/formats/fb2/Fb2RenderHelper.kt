package com.akergez.superreader2000.shared.formats.fb2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
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

    private val imageMap = mutableMapOf<String, InlineTextContent>()
    private val pageCache = mutableMapOf<Int, MutableStateFlow<Fb2BookContent>>()

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

    init {
        scope.launch {
            val elementCount: Int
            synchronized(lockParse) {
                val result = parseFb2Content(fb2File.file)
                imageMap.putAll(result.images)
                tableOfContents.update { result.toc }
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
                yield() // кооперативная отмена — раз на страницу

                // Берём кусок ~2× одной страницы. Если он целиком поместился
                // (маловероятно при правильной оценке), удваиваем и повторяем.
                var chunkSize = baseChunkSize
                var chunkEnd: Int
                var layout: androidx.compose.ui.text.TextLayoutResult
                while (true) {
                    chunkEnd = (charOffset + chunkSize).coerceAtMost(fullText.length)
                    layout = textMeasurer.measure(
                        text        = fullText.subSequence(charOffset, chunkEnd),
                        style       = style,
                        constraints = Constraints(minWidth = w, maxWidth = w, minHeight = 0, maxHeight = h),
                        softWrap    = true,
                    )
                    if (layout.didOverflowHeight || chunkEnd == fullText.length) break
                    chunkSize *= 2
                }
                val chunk = fullText.subSequence(charOffset, chunkEnd)

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

                commitPageData(lastSlicedPageIndex, pageText, commands, config)
                charOffset += pageEndInChunk
                lastSlicedPageIndex++

                if (!layout.didOverflowHeight) {
                    isAllPageSliced = true
                    pagesCount.update { lastSlicedPageIndex }
                    return
                }

                if (lastSlicedPageIndex > targetPageIndex) {
                    // Пересчитываем оценку каждый раз по реальному соотношению символов/страниц
                    val charsPerPage = (charOffset / lastSlicedPageIndex).coerceAtLeast(1)
                    val estimated = (fullText.length / charsPerPage).coerceAtLeast(lastSlicedPageIndex)
                    pagesCount.update { estimated }
                    return
                }
            }

            isAllPageSliced = true
            pagesCount.update { lastSlicedPageIndex }
        }
    }

    /** Строит DrawCommand из уже измеренного layout — без повторного measure().
     *  [lastLine] — последняя строка для отрисовки (включительно). */
    private fun buildDrawCommandsFromLayout(
        layout: androidx.compose.ui.text.TextLayoutResult,
        text: AnnotatedString,
        config: RenderConfig,
        lastLine: Int = layout.lineCount - 1,
    ): List<DrawCommand> {
        val commands = mutableListOf<DrawCommand>()
        for (line in 0..lastLine) {
            val lineText = text.subSequence(layout.getLineStart(line), layout.getLineEnd(line))
            if (lineText.text.isBlank() && lineText.text != "\n") continue
            commands += DrawCommand.DrawText(
                text  = lineText,
                x     = PADDING_H + layout.getLineLeft(line),
                y     = PADDING_V + layout.getLineBaseline(line).toFloat(),
                style = TextStyle(fontSize = config.fontSize, fontFamily = config.fontFamily),
            )
        }
        return commands
    }

    private fun commitPageData(index: Int, pageText: AnnotatedString, commands: List<DrawCommand>, config: RenderConfig) {
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
    }

    /** Собирает всю книгу в одну AnnotatedString (один раз после парсинга). */
    private fun buildFullText(elements: List<FB2Element>): AnnotatedString = buildAnnotatedString {
        elements.forEachIndexed { i, element ->
            if (i > 0) append(' ')
            append(element.toStyledString())
        }
    }

    private data class ParseResult(
        val elements: List<FB2Element>,
        val images: Map<String, InlineTextContent>,
        val toc: List<TOCEntry>,
    )

    private fun parseFb2Content(fb2File: File): ParseResult {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(fb2File)
            doc.documentElement.normalize()

            val tocEntries = mutableListOf<TOCEntry>()
            val elements   = mutableListOf<FB2Element>()
            val images     = mutableMapOf<String, InlineTextContent>()

            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name != "notes") {
                    elements.addAll(extractTextContent(body, tocEntries))
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
                        images[id] = InlineTextContent(
                            Placeholder(300.sp, 300.sp, PlaceholderVerticalAlign.TextCenter)
                        ) {
                            val bytes = Base64.decode(base64Content)
                            androidx.compose.foundation.Image(
                                bitmap             = bytes.decodeToImageBitmap(),
                                contentDescription = null,
                                modifier           = androidx.compose.ui.Modifier.fillMaxSize(),
                                contentScale       = androidx.compose.ui.layout.ContentScale.Fit,
                            )
                        }
                    }
                }
            }

            ParseResult(elements, images, tocEntries.distinctBy { it.title })
        } catch (e: Exception) {
            e.printStackTrace()
            ParseResult(
                elements = listOf(FB2ColoredText("Can't parse book", Color.Red)),
                images   = emptyMap(),
                toc      = emptyList(),
            )
        }
    }

    private fun extractTextContent(node: Node, tocEntries: MutableList<TOCEntry>): List<FB2Element> {
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
                    val extracted = extractTextContent(child, tocEntries)
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
                                    pageIndex = elements.size,
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
                            if (href != null && href.startsWith("#")) {
                                elements += FB2Image(imageId = href.substring(1), description = href)
                            }
                        }
                        else -> elements.addAll(extractTextContent(child, tocEntries))
                    }
                }
            }
        }
        return elements
    }

    private fun combineAnnotatedStrings(list: List<AnnotatedString>): AnnotatedString =
        buildAnnotatedString { list.forEach { append(it) } }

    companion object {
        private const val PADDING_H   = 32f
        private const val PADDING_V   = 32f
        private const val DEBOUNCE_MS = 300L
    }
}
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
import kotlinx.coroutines.launch
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.encoding.Base64

data class RenderConfig(
    val pageWidth: Int,
    val pageHeight: Int,
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

    private var parsedElements: List<FB2Element> = emptyList()
    private val imageMap = mutableMapOf<String, InlineTextContent>()
    private val pageCache = mutableMapOf<Int, MutableStateFlow<Fb2BookContent>>()

    private var currentPageText = AnnotatedString("")
    private var lastSlicedPageIndex = 0
    private var isAllPageSliced = false
    private var elementQueue: ArrayDeque<FB2Element> = ArrayDeque()

    private val lockParse = Any()
    private val lockPaginate = Any()
    private var paginationJob: Job? = null

    val totalWords = MutableStateFlow<Int?>(null)
    val pagesCount = MutableStateFlow<Int?>(null)
    val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())
    val pageWordSum = mutableListOf<Int>()

    init {
        scope.launch {
            synchronized(lockParse) {
                val result = parseFb2Content(fb2File.file)
                parsedElements = result.elements
                imageMap.putAll(result.images)
                tableOfContents.update { result.toc }
                totalWords.update { result.elements.size }
            }
            println(">>> parsed ${parsedElements.size} elements, config=${renderConfig.value}")

            // Явно запускаем пагинацию если конфиг уже валиден на момент старта.
            // collectLatest не эмитит текущее значение повторно после подписки —
            // поэтому первый запуск делаем вручную.
            if (renderConfig.value.isValid) {
                triggerRepagination()
            }

            // Следим за изменениями конфига (шрифт, ориентация).
            // filter отсекает невалидные конфиги до Constraints.
            // collectLatest отменяет пагинацию при новом валидном конфиге.
            renderConfig
                .filter { it.isValid }
                .collectLatest {
                    delay(DEBOUNCE_MS)
                    if (renderConfig.value.isValid) {
                        triggerRepagination()
                    }
                }
        }
    }

    override fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<Fb2BookContent> {
        pageCache[pageIndex]?.let { flow ->
            if (flow.value.drawCommands.isNotEmpty()) return flow
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

    fun getPageIndexByWordOffset(offset: Int): Int {
        var index = 0
        while (!isAllPageSliced) {
            if (pageWordSum.size > index && pageWordSum[index] > offset) return index
            generatePagesUpTo(++index)
        }
        return pageWordSum.indexOfFirst { it >= offset }.coerceAtLeast(0)
    }

    private fun triggerRepagination() {
        println(">>> triggerRepagination config=${renderConfig.value}")
        if (!renderConfig.value.isValid) {
            println(">>> triggerRepagination SKIPPED - config invalid")
            return
        }

        paginationJob?.cancel()
        synchronized(lockPaginate) {
            pageCache.values.forEach { flow ->
                flow.update { it.copy(drawCommands = emptyList(), totalPages = null) }
            }
            currentPageText = AnnotatedString("")
            lastSlicedPageIndex = 0
            isAllPageSliced = false
            pageWordSum.clear()
            elementQueue = ArrayDeque(parsedElements)
        }

        paginationJob = scope.launch { generatePagesUpTo(3) }
    }

    private fun generatePagesUpTo(targetPageIndex: Int) {
        synchronized(lockPaginate) {
            if (isAllPageSliced) return
            if (pageCache[targetPageIndex]?.value?.drawCommands?.isNotEmpty() == true) return

            val config = renderConfig.value
            if (!config.isValid) return  // ← финальный guard внутри synchronized

            while (elementQueue.isNotEmpty()) {
                val element = elementQueue.removeFirst()

                val candidate = buildAnnotatedString {
                    if (currentPageText.isNotEmpty()) {
                        append(currentPageText)
                        append(' ')
                    }
                    append(element.toStyledString())
                }

                if (fitsOnPage(candidate, config)) {
                    currentPageText = candidate
                } else {
                    commitPage(lastSlicedPageIndex, currentPageText, config)
                    lastSlicedPageIndex++

                    if (pagesCount.value == null && currentPageText.isNotEmpty()) {
                        val wordsPerPage = currentPageText.split(" ").size.coerceAtLeast(1)
                        pagesCount.update { elementQueue.size / wordsPerPage + lastSlicedPageIndex }
                    }

                    currentPageText = element.toStyledString()

                    if (lastSlicedPageIndex > targetPageIndex) {
                        pagesCount.value?.let { est ->
                            if (lastSlicedPageIndex >= est) pagesCount.update { est + 10 }
                        }
                        return
                    }
                }
            }

            if (currentPageText.isNotEmpty()) {
                commitPage(lastSlicedPageIndex, currentPageText, config)
                isAllPageSliced = true
                pagesCount.update { lastSlicedPageIndex + 1 }
            }
        }
    }

    private fun fitsOnPage(text: AnnotatedString, config: RenderConfig): Boolean {
        val w = config.pageWidth.coerceAtLeast(1)
        val h = config.pageHeight.coerceAtLeast(1)
        val result = textMeasurer.measure(
            text = text,
            style = TextStyle(
                fontSize = config.fontSize,
                fontFamily = config.fontFamily,
                lineHeight = config.fontSize * config.lineHeightMultiplier,
                textAlign = TextAlign.Justify,
            ),
            constraints = Constraints(
                minWidth = w,
                maxWidth = w,
                minHeight = 0,
                maxHeight = h,
            ),
            softWrap = true,
        )
        return !result.didOverflowHeight
    }

    private fun buildDrawCommands(text: AnnotatedString, config: RenderConfig): List<DrawCommand> {
        val w = config.pageWidth.coerceAtLeast(1)
        val h = config.pageHeight.coerceAtLeast(1)
        val result = textMeasurer.measure(
            text = text,
            style = TextStyle(
                fontSize = config.fontSize,
                fontFamily = config.fontFamily,
                lineHeight = config.fontSize * config.lineHeightMultiplier,
                textAlign = TextAlign.Justify,
            ),
            constraints = Constraints.fixed(w, h),
            softWrap = true,
        )

        val commands = mutableListOf<DrawCommand>()
        for (line in 0 until result.lineCount) {
            val lineText = text.subSequence(result.getLineStart(line), result.getLineEnd(line))
            if (lineText.text.isBlank() && lineText.text != "\n") continue
            commands += DrawCommand.DrawText(
                text  = lineText,
                x     = PADDING_H + result.getLineLeft(line),
                y     = PADDING_V + result.getLineBaseline(line).toFloat(),
                style = TextStyle(fontSize = config.fontSize, fontFamily = config.fontFamily),
            )
        }
        return commands
    }

    private fun commitPage(index: Int, text: AnnotatedString, config: RenderConfig) {
        val commands    = buildDrawCommands(text, config)
        println(">>> commitPage index=$index commands=${commands.size} words=${text.text.take(30)}")
        val wordsOnPage = text.text.split(" ").filter { it.isNotBlank() }.size

        while (pageWordSum.size < index) pageWordSum.add(pageWordSum.lastOrNull() ?: 0)
        pageWordSum.add(wordsOnPage + (pageWordSum.lastOrNull() ?: 0))

        val content = Fb2BookContent(
            drawCommands = commands,
            imageMap     = imageMap,
            pageIndex    = index,
            totalPages   = pagesCount.value,
        )
        if (pageCache.containsKey(index)) {
            pageCache[index]!!.update { content }
        } else {
            pageCache[index] = MutableStateFlow(content)
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
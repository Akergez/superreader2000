package com.example.bottomapp.shared.formats.fb2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.IRenderHelper
import com.example.bottomapp.shared.formats.pdf.TOCEntry
import com.example.bottomapp.shared.ui.Fb2BookContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.w3c.dom.Node
import java.io.File
import java.util.LinkedList
import java.util.Queue
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.forEach
import kotlin.io.encoding.Base64

actual class Fb2RenderHelper actual constructor(
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val fb2File: FileWrapper,
) : IRenderHelper<Fb2BookContent> {
    private var cachedContent: Queue<FB2Element>? = null
    private val pageCache = mutableMapOf<Int, MutableStateFlow<Fb2BookContent>>()
    actual val pageWordSum = mutableListOf<Int>()
    actual val totalWords = MutableStateFlow<Int?>(null)
    actual val pagesCount = MutableStateFlow<Int?>(null)
    private val imageMap = mutableMapOf<String, InlineTextContent>()
    private var currentPage = AnnotatedString("")
    private var lastSlicedPageIndex = 0
    private var isAllPageSliced = false
    private val lock = Any()
    private val lockParseFile = Any()
    actual val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            synchronized(lockParseFile) {
                cachedContent = LinkedList(parseFb2Content(fb2File.file))
            }
            openPage(1, false)
        }
    }

    override fun openPage(
        pageIndex: Int,
        applyInversion: Boolean
    ): StateFlow<Fb2BookContent> {
        pageCache[pageIndex]?.let {
            return it
        }

        if (isAllPageSliced) {
            return MutableStateFlow(Fb2BookContent(androidx.compose.ui.text.AnnotatedString(""), imageMap))
        }

        val pageText = MutableStateFlow(Fb2BookContent(null, imageMap))
        CoroutineScope(Dispatchers.IO).launch {
            if (pageCache[pageIndex] == null || pageCache[pageIndex]?.value?.text == null) {
                pageCache[pageIndex] = pageText
                generatePagesUpTo(pageIndex + 2)
            }
        }
        return pageText
    }

    private fun generatePagesUpTo(targetPageIndex: Int) {
        synchronized(lockParseFile) {
            if (cachedContent == null) {
                cachedContent = LinkedList(parseFb2Content(fb2File.file))
            }
        }
        synchronized(lock) {
            if (pageCache[targetPageIndex]?.value?.text != null) {
                return
            }

            while (true) {
                if (cachedContent.isNullOrEmpty()) break

                val word = cachedContent!!.remove()

                val annotatedStringBuilder = buildAnnotatedString {
                    if (currentPage.isNotEmpty()) {
                        append(currentPage)
                        append(' ')
                    }
                    append(word.toStyledString())
                }

                val result = textMeasurer.measure(
                    text = annotatedStringBuilder,
                    style = TextStyle(textAlign = TextAlign.Companion.Justify),
                    constraints = Constraints(
                        minWidth = maxWidth,
                        maxWidth = maxWidth,
                        minHeight = maxHeight,
                        maxHeight = maxHeight
                    ),
                    softWrap = true
                )

                if (!result.didOverflowHeight) {
                    currentPage = annotatedStringBuilder
                } else {
                    addOrUpdatePage(lastSlicedPageIndex, currentPage)
                    lastSlicedPageIndex++

                    if (pagesCount.value == null) {
                        pagesCount.update { cachedContent!!.size / currentPage.split(" ").size }
                    }

                    currentPage = word.toStyledString()

                    if (lastSlicedPageIndex > targetPageIndex && pagesCount.value != null) {
                        if (lastSlicedPageIndex >= pagesCount.value!!) {
                            pagesCount.update { pagesCount.value!! + 1 }
                        }
                        break
                    }
                }
            }

            if (currentPage.isNotEmpty() && lastSlicedPageIndex <= targetPageIndex) {
                addOrUpdatePage(lastSlicedPageIndex, currentPage)
                isAllPageSliced = true
            }
        }
    }

    private fun addOrUpdatePage(index: Int, text: AnnotatedString) {
        if (pageCache.containsKey(index)) {
            pageCache[index]!!.update { Fb2BookContent(text, imageMap) }
        } else {
            pageCache[index] = MutableStateFlow(Fb2BookContent(text, imageMap))
        }

        val wordsPerPage = text.split(" ").size
        if (index > 0) {
            pageWordSum.add(index, pageWordSum[index - 1] + wordsPerPage)
        } else {
            pageWordSum.add(index, wordsPerPage)
        }
    }

    override fun close() {
        pageCache.clear()
    }

    override fun getBookType(): FormatType {
        return FormatType.FB2
    }

    override fun getTableOfContents(): StateFlow<List<TOCEntry>> {
        return tableOfContents
    }

    actual fun getPageIndexByWordOffset(offset: Int): Int {
        var index = 0
        while (!isAllPageSliced) {
            if (pageWordSum.size > index && pageWordSum[index] > offset) {
                return index
            } else {
                generatePagesUpTo(++index)
            }
        }
        return 0
    }

    private fun parseFb2Content(fb2File: File): List<FB2Element> {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(fb2File)

            doc.documentElement.normalize()

            val tocEntries = mutableListOf<TOCEntry>()
            val elements = mutableListOf<FB2Element>()

            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name == "notes") {
                    elements.addAll(extractTextContent(body, tocEntries))
                }
            }

            val binaryElements = doc.getElementsByTagName("binary")
            for (i in 0 until binaryElements.length) {
                val binaryNode = binaryElements.item(i)
                val contentType = binaryNode.attributes?.getNamedItem("content-type")?.nodeValue
                val id = binaryNode.attributes?.getNamedItem("id")?.nodeValue

                if (contentType?.startsWith("image/") == true && id != null) {
                    val base64Content = binaryNode.textContent?.trim() ?: ""
                    if (base64Content.isNotEmpty()) {
                        imageMap[id] = InlineTextContent(
                            Placeholder(
                                300.sp,
                                300.sp,
                                PlaceholderVerticalAlign.Companion.TextCenter
                            )
                        ) {
                            val bytes = Base64.decode(base64Content)
                            Image(
                                bitmap = bytes.decodeToImageBitmap(),
                                modifier = Modifier.Companion.fillMaxSize(),
                                contentDescription = null,
                                contentScale = ContentScale.Companion.Fit
                            )
                        }
                    }
                }
            }

            totalWords.update { elements.size }
            tableOfContents.update { tocEntries.distinctBy { it.title } }
            return elements
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf(FB2ColoredText("Can't parse book", Color.Companion.Red))
        }
    }

    private fun extractTextContent(
        node: Node,
        tocEntries: MutableList<TOCEntry>
    ): List<FB2Element> {
        val elements = mutableListOf<FB2Element>()

        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)

            when (child.nodeType) {
                Node.TEXT_NODE -> {
                    val text = child.nodeValue?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        val words = text.split(' ', '\n', '\r')
                            .filter { it.isNotEmpty() && it.isNotBlank() }
                            .map { FB2Text(it.trim()) }

                        elements.addAll(words)
                    }
                }

                Node.ELEMENT_NODE -> {
                    val extracted = extractTextContent(child, tocEntries)
                    when (child.nodeName) {
                        "empty-line" -> elements.add(FB2Text(""))
                        "emphasis" -> elements.addAll(extracted.map { FB2ItalicText(it) })
                        "title" -> {
                            elements.add(FB2Text("\n"))
                            elements.addAll(extracted.map { FB2BoldText(it) })
                            elements.add(FB2Text("\n"))

                            if (extracted.isNotEmpty()) {
                                val tocEntry = TOCEntry(
                                    title = child.textContent,
                                    pageIndex = elements.size,
                                    children = emptyList()
                                )
                                tocEntries.add(tocEntry)
                            }
                        }

                        "strong" -> elements.addAll(extracted.map { FB2BoldText(it) })
                        "subtitle" -> {
                            elements.add(FB2Text("\n"))
                            elements.add(FB2SubtitleText(combineAnnotatedStrings(extracted.map { it.toStyledString() })))
                            elements.add(FB2Text("\n"))
                        }

                        "code" -> elements.addAll(extracted.map { FB2CodeText(it) })
                        "p" -> {
                            if (extracted.isNotEmpty()) {
                                elements.add(FB2Paragraph(extracted[0]))
                                elements.addAll(extracted.drop(1).map { FB2Text(it) })
                            }
                        }

                        "a" -> {
                            val href = child.attributes?.getNamedItem("href")?.nodeValue
                            if (extracted.isNotEmpty() && href != null) {
                                elements.addAll(extracted.map { FB2ColoredText(it, Color.Companion.Blue) })
                            }
                        }

                        "image" -> {
                            val href = child.attributes?.getNamedItem("l:href")?.nodeValue
                            if (href != null && href.startsWith("#")) {
                                elements.add(
                                    FB2Image(
                                        imageId = href.substring(1),
                                        description = href
                                    )
                                )

                            }
                        }

                        else -> elements.addAll(extractTextContent(child, tocEntries))
                    }
                }
            }
        }
        return elements
    }

    private fun combineAnnotatedStrings(annotatedStrings: List<AnnotatedString>): AnnotatedString {
        return buildAnnotatedString {
            annotatedStrings.forEach { annotatedString ->
                append(annotatedString)
            }
        }
    }
}
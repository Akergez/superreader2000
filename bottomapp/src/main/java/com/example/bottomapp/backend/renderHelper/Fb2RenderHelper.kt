package com.example.bottomapp.backend.renderHelper

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.backend.renderHelper.fb2.FB2BoldText
import com.example.bottomapp.backend.renderHelper.fb2.FB2CodeText
import com.example.bottomapp.backend.renderHelper.fb2.FB2ColoredText
import com.example.bottomapp.backend.renderHelper.fb2.FB2Element
import com.example.bottomapp.backend.renderHelper.fb2.FB2Image
import com.example.bottomapp.backend.renderHelper.fb2.FB2ItalicText
import com.example.bottomapp.backend.renderHelper.fb2.FB2Paragraph
import com.example.bottomapp.backend.renderHelper.fb2.FB2SubtitleText
import com.example.bottomapp.backend.renderHelper.fb2.FB2Text
import com.example.bottomapp.ui.readerscreens.Fb2BookContent
import com.example.bottomapp.ui.readerscreens.IBookContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.w3c.dom.Node
import java.io.File
import java.util.LinkedList
import java.util.Queue
import javax.xml.parsers.DocumentBuilderFactory

class Fb2RenderHelper(
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val fb2File: File
) : IRenderHelper {
    private var cachedContent: Queue<FB2Element>? = null
    private val pageCache = mutableMapOf<Int, MutableStateFlow<Fb2BookContent>>()
    val pageWordSum = mutableListOf<Int>()
    var pagesCount = MutableStateFlow(100)
    private val imageMap = mutableMapOf<String, InlineTextContent>()
    private var currentPage = AnnotatedString("")
    private var lastSlicedPageIndex = 0
    private var isAllPageSliced = false
    private val lock = Any()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cachedContent = LinkedList(parseFb2Content(fb2File))
        }
    }

    override fun openPage(pageIndex: Int): StateFlow<IBookContent> {
        Log.d("Fb2RenderHelper", "OpenPage: $pageIndex")

        pageCache[pageIndex]?.let {
            return it
        }

        if (isAllPageSliced) {
            return MutableStateFlow(Fb2BookContent(AnnotatedString(""), imageMap))
        }

        val pageText = MutableStateFlow(Fb2BookContent(null, imageMap))
        CoroutineScope(Dispatchers.IO).launch {
            synchronized(lock) {
                if (pageCache[pageIndex] == null || pageCache[pageIndex]?.value?.text == null) {
                    pageCache[pageIndex] = pageText
                    generatePagesUpTo(pageIndex + 2)
                }
            }
        }
        return pageText
    }

    private fun generatePagesUpTo(targetPageIndex: Int) {
        Log.d("Fb2RenderHelper", "GeneratePagesUpTo: $targetPageIndex")

        if (cachedContent.isNullOrEmpty()) {
            cachedContent = LinkedList(parseFb2Content(fb2File))
        }

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

            // Измерение текста
            val result = textMeasurer.measure(
                text = annotatedStringBuilder,
                style = TextStyle(textAlign = TextAlign.Justify),
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
                // Логика разбиения страниц
                addOrUpdatePage(lastSlicedPageIndex, currentPage)
                lastSlicedPageIndex++

                if (pagesCount.value == 1) {
                    pagesCount.update { cachedContent!!.size / currentPage.split(" ").size }
                }

                // Сброс текущей страницы
                currentPage = word.toStyledString()

                // Проверка достижения целевой страницы
                if (lastSlicedPageIndex > targetPageIndex) {
                    if (lastSlicedPageIndex >= pagesCount.value) {
                        pagesCount.update { pagesCount.value + 1 }
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

    private fun parseFb2Content(fb2File: File): List<FB2Element> {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(fb2File)

            // Normalize the document
            doc.documentElement.normalize()

            val elements = mutableListOf<FB2Element>()
            // Find all description elements
            val descriptionElements = doc.getElementsByTagName("description")
            for (i in 0 until descriptionElements.length) {
                val description = descriptionElements.item(i)
                elements.addAll(extractTextContent(description))
            }

            // Find all body elements
            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)
                // Check if this is the main body of the text
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name == "notes") {
                    elements.addAll(extractTextContent(body))
                }
            }

            // Найти и спарсить все binary изображения
            val binaryElements = doc.getElementsByTagName("binary")
            for (i in 0 until binaryElements.length) {
                val binaryNode = binaryElements.item(i)
                val contentType = binaryNode.attributes?.getNamedItem("content-type")?.nodeValue
                val id = binaryNode.attributes?.getNamedItem("id")?.nodeValue

                // Проверяем, что это изображение
                if (contentType?.startsWith("image/") == true && id != null) {
                    val base64Content = binaryNode.textContent?.trim() ?: ""
                    if (base64Content.isNotEmpty()) {
                        imageMap[id] = InlineTextContent(
                            Placeholder(
                                300.sp,
                                300.sp,
                                PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            val bytes = Base64.decode(base64Content, Base64.DEFAULT)
                            Image(
                                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    .asImageBitmap(),
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            return elements
        } catch (e: Exception) {
            Log.e("FB2Parser", "Error parsing FB2: ${e.message}")
            e.printStackTrace()
            return listOf(FB2ColoredText("Can't parse book", Color.Red))
        }
    }

    private fun extractTextContent(node: Node): List<FB2Element> {
        val elements = mutableListOf<FB2Element>()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)

            when (child.nodeType) {
                Node.TEXT_NODE -> {
                    // строки вроде как возвращаются только тут
                    val text = child.nodeValue?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        val words = text.split(' ', '\n', '\r')
                            .filter { it.isNotEmpty() && it.isNotBlank() }
                            .map { FB2Text(it.trim()) }

                        elements.addAll(words) // Обычный текст
                    }
                }

                Node.ELEMENT_NODE -> {
                    val extracted = extractTextContent(child)
                    when (child.nodeName) {
                        "empty-line" -> elements.add(FB2Text(""))
                        "emphasis" -> elements.addAll(extracted.map { FB2ItalicText(it) })
                        "title" -> {
                            elements.add(FB2Text("\n"))
                            elements.addAll(extracted.map { FB2BoldText(it) })
                            elements.add(FB2Text("\n"))
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
                                elements.addAll(extracted.map { FB2ColoredText(it, Color.Blue) })
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

                        else -> elements.addAll(extractTextContent(child))
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
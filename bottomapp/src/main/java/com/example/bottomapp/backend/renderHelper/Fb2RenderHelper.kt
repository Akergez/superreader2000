package com.example.bottomapp.backend.renderHelper

import android.util.Log
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.FormatType
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
    private var cachedContent: Queue<String>? = null
    private val pageCache = mutableMapOf<Int, MutableStateFlow<Fb2BookContent>>()
    val pageWordSum = mutableListOf<Int>()
    var pagesCount = MutableStateFlow(100)
    private var currentPage = StringBuilder()
    private var lastSlicedPageIndex = 0
    private var isAllPageSliced = false
    private val lock = Any()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cachedContent = LinkedList(parseFb2Content(fb2File).split(" ", "\r\n", "\n"))
        }
    }

    override fun openPage(pageIndex: Int): StateFlow<IBookContent> {
        Log.d("Fb2RenderHelper", "OpenPage: $pageIndex")

        pageCache[pageIndex]?.let {
            return it
        }

        if (isAllPageSliced) {
            return MutableStateFlow(Fb2BookContent(""))
        }

        val pageText = MutableStateFlow(Fb2BookContent(null))
        CoroutineScope(Dispatchers.IO).launch {
            synchronized(lock) {
                if (pageCache[pageIndex] == null || pageCache[pageIndex]?.value?.text == null) {
                    pageCache[pageIndex] = pageText
                    generatePagesUpTo(pageIndex)
                }
            }
        }
        return pageText
    }

    private fun generatePagesUpTo(targetPageIndex: Int) {
        Log.d("Fb2RenderHelper", "GeneratePagesUpTo: $targetPageIndex")

        if(cachedContent.isNullOrEmpty()){
            cachedContent = LinkedList(parseFb2Content(fb2File).split(" ", "\r\n", "\n"))
        }

        if (pageCache[targetPageIndex]?.value?.text != null) {
            return
        }

        while (true) {
            val word = cachedContent!!.remove()
            val testLine = if (currentPage.isEmpty()) word else "$currentPage $word"

            val result = textMeasurer.measure(
                text = testLine,
                style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Left),
                constraints = Constraints(
                    minWidth = maxWidth,
                    maxWidth = maxWidth,
                    minHeight = maxHeight,
                    maxHeight = maxHeight
                ),
                softWrap = true
            )

            if (!result.didOverflowHeight) {
                currentPage.append(if (currentPage.isEmpty()) word else " $word")
            } else {
                addOrUpdatePage(lastSlicedPageIndex, currentPage.toString().trim())
                lastSlicedPageIndex++

                if (pagesCount.value == 1) {
                    // Примерно считаем, сколько страниц будет в книге
                    pagesCount.update { cachedContent!!.size / currentPage.split(" ", " ").size }
                }

                currentPage = StringBuilder()
                currentPage.append(word)

                // Если достигли нужной страницы, можем остановиться
                if (lastSlicedPageIndex > targetPageIndex) {
                    if (lastSlicedPageIndex >= pagesCount.value) {
                        pagesCount.update { pagesCount.value + 1 }
                    }
                    break
                }
            }
        }

        if (currentPage.isNotEmpty() && lastSlicedPageIndex <= targetPageIndex) {
            addOrUpdatePage(lastSlicedPageIndex, currentPage.toString().trim())
            isAllPageSliced = true
        }
    }

    private fun addOrUpdatePage(index: Int, text: String) {
        if (pageCache.containsKey(index)) {
            pageCache[index]!!.update {  Fb2BookContent(text)}
        } else {
            pageCache[index] = MutableStateFlow(Fb2BookContent(text))
        }

        val wordsPerPage = text.split(" ").size
        if (index > 0) {
            pageWordSum.add(index, pageWordSum[index - 1] + wordsPerPage)
        } else {
            pageWordSum.add(index, wordsPerPage)
        }
    }

    override fun close() {
        Log.d("Fb2RenderHelper", "Clear cache and content")
        pageCache.clear()
    }

    override fun getBookType(): FormatType {
        return FormatType.FB2
    }

    private fun parseFb2Content(fb2File: File): String {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(fb2File)

            // Normalize the document
            doc.documentElement.normalize()

            val stringBuilder = StringBuilder()

            // Find all body elements
            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)

                // Check if this is the main body of the text
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name == "notes") {
                    parseBodyContent(body, stringBuilder)
                }
            }

            return stringBuilder.toString()
        } catch (e: Exception) {
            Log.e("FB2Parser", "Error parsing FB2: ${e.message}")
            e.printStackTrace()
            return "Ошибка при парсинге книги: ${e.message}"
        }
    }

    private fun parseBodyContent(node: Node, stringBuilder: StringBuilder) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            when (child.nodeName) {
                "section" -> parseBodyContent(child, stringBuilder)
                "title" -> {
                    val title = extractTextContent(child)
                    if (title.isNotEmpty()) {
                        stringBuilder.append(title.uppercase()).append("\n\n")
                    }
                }

                "p" -> {
                    val paragraph = extractTextContent(child)
                    if (paragraph.isNotEmpty()) {
                        stringBuilder.append(paragraph).append("\n\n")
                    }
                }

                "empty-line" -> {
                    stringBuilder.append("\n")
                }
            }
        }
    }

    private fun extractTextContent(node: Node): String {
        val stringBuilder = StringBuilder()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)

            when (child.nodeType) {
                Node.TEXT_NODE -> {
                    val text = child.nodeValue?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        stringBuilder.append(text).append(" ")
                    }
                }

                Node.ELEMENT_NODE -> {
                    when (child.nodeName) {
                        "emphasis", "strong", "sup", "sub", "code", "a" -> {
                            val text = extractTextContent(child)
                            if (text.isNotEmpty()) {
                                stringBuilder.append(text).append(" ")
                            }
                        }
                    }
                }
            }
        }
        return stringBuilder.toString().trim()
    }
}
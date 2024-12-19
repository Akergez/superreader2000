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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Fb2RenderHelper(
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val fb2File: File
) : IRenderHelper {

    val pagesCount = MutableStateFlow<Int>(0)
    private var cachedContent: String? = null
    private val pageCache = mutableMapOf<Int, String>()
    private var isAllPageSliced = false

    override fun openPage(pageIndex: Int): MutableStateFlow<IBookContent> {
        // Ленивая генерация страниц по требованию
        generatePagesUpTo(pageIndex)

        val pageText = pageCache[pageIndex]

        if (pageText == null && isAllPageSliced) {
            return MutableStateFlow(Fb2BookContent(null))
        }

        return MutableStateFlow(Fb2BookContent(pageText))
    }

    private fun generatePagesUpTo(targetPageIndex: Int) {
        // Если контент еще не загружен, загружаем
        if (cachedContent == null) {
            cachedContent = parseFb2Content(fb2File)
        }

        // Если страницы уже сгенерированы до нужного индекса, выходим
        if (pageCache.containsKey(targetPageIndex)) {
            return
        }

        val words = cachedContent?.split(" ", " ") ?: return
        var currentPage = StringBuilder()
        var currentPageIndex = 0

        for (word in words) {
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
                pageCache[currentPageIndex] = currentPage.toString().trim()
                currentPageIndex++

                if (pagesCount.value == 1) {
                    // примерно считаем, сколько страниц будет в книге
                    pagesCount.update { words.size / currentPage.split(" ", " ").size }
                }

                // Если достигли нужной страницы, можем остановиться
                if (currentPageIndex > targetPageIndex) {
                    if (currentPageIndex >= pagesCount.value) {
                        pagesCount.update { pagesCount.value + 1 }
                    }
                    break
                }

                currentPage = StringBuilder()
                currentPage.append(word)
            }
        }

        // Добавляем последнюю страницу, если она не пустая
        if (currentPage.isNotEmpty() && currentPageIndex <= targetPageIndex) {
            pageCache[currentPageIndex] = currentPage.toString().trim()
            isAllPageSliced = true
        }
    }

    override fun close() {
        pageCache.clear()
        cachedContent = null
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
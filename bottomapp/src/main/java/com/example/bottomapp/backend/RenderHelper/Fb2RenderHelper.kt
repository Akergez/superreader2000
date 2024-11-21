package com.example.bottomapp.backend.RenderHelper

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.FormatType
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Fb2RenderHelper(
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val fb2File: File
) : IRenderHelper {

    public val pages: MutableList<String> = mutableListOf()
    private var cachedContent: String? = null

    override fun getPageCount(): Int = pages.size
    override fun openPage(pageIndex: Int): Bitmap {
        return Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
    }

    fun Init(fontSize: Int){
        cachedContent = parseFb2Content(fb2File)

        val words = cachedContent!!.split(" ", " ")
        var currentPage = StringBuilder()

        for (word in words) {
            val testLine = if (currentPage.isEmpty()) word else "$currentPage $word"

            val result = textMeasurer.measure(
                text = testLine,
                style = TextStyle(fontSize = fontSize.sp, textAlign = TextAlign.Center),
                constraints = Constraints(
                    minWidth = maxWidth,
                    maxWidth = maxWidth,
                    minHeight = maxHeight,
                    maxHeight = maxHeight
                ),
                softWrap = true,
            )

            if (!result.didOverflowHeight) {
                currentPage.append(if (currentPage.isEmpty()) word else " $word")
            } else {
                pages.add(currentPage.toString().trim())
                currentPage = StringBuilder()
                currentPage.append(word)
            }
        }
        if (currentPage.isNotEmpty()) {
            pages.add(currentPage.toString().trim())
        }
    }

    override fun openPagedText(pageIndex: Int): String {
        return pages[pageIndex]
    }

    override fun close() {
        // No-op for now, but you could add any necessary cleanup code here
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
            return "Ошибка при парсинге книги: ${e .message}"
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
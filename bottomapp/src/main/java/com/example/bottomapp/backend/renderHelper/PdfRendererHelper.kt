package com.example.bottomapp.backend.renderHelper

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.PdfBookContent
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererHelper(val pdfFile: suspend () -> File) : IRenderHelper {
    val pdfRenderer = MutableStateFlow<PdfRenderer?>(null)
    val pagesCount = MutableStateFlow(0)
    private val bitmapCache = mutableMapOf<Int, MutableStateFlow<IBookContent>>()
    private val lock = Any()
    private val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val parcelFileDescriptor =
                ParcelFileDescriptor.open(pdfFile(), ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer.value = PdfRenderer(parcelFileDescriptor)
            pagesCount.value = pdfRenderer.value!!.pageCount
            extractTableOfContents()
        }
    }

    override fun openPage(pageIndex: Int): MutableStateFlow<IBookContent> {
        bitmapCache[pageIndex]?.let {
            return it
        }
        val bitmapStateFlow = MutableStateFlow<IBookContent>(PdfBookContent(null))
        bitmapCache[pageIndex] = bitmapStateFlow
        CoroutineScope(Dispatchers.IO).launch {
            var bitmap: Bitmap?
            synchronized(lock) {
                val page = pdfRenderer.value?.openPage(pageIndex)
                bitmap =
                    Bitmap.createBitmap(page!!.width * 5, page.height * 5, Bitmap.Config.ARGB_8888)
                val rect = Rect(0, 0, page.width * 5, page.height * 5)
                page.render(bitmap!!, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
            }
            withContext(Dispatchers.Main) {
                bitmapStateFlow.value = PdfBookContent(bitmap)
            }
        }
        return bitmapStateFlow
    }

    override fun close() {
        synchronized(lock) {
            pdfRenderer.value?.close()
            bitmapCache.clear()
        }
    }

    override fun getBookType(): FormatType {
        return FormatType.PDF
    }

    fun getTableOfContents(): StateFlow<List<TOCEntry>> = tableOfContents

    private fun extractTableOfContents() {
        CoroutineScope(Dispatchers.IO).launch {
            val tocEntries = mutableListOf<TOCEntry>()

            try {
                val file = pdfFile()
                PDDocument.load(file).use { document ->
                    val catalog = document.documentCatalog
                    val outline = catalog.documentOutline
                    if (outline != null) {
                        var currentNode = outline.firstChild
                        while (currentNode != null) {
                            val chapter = parseOutlineItem(currentNode, "", document)
                            tocEntries.add(chapter)
                            currentNode = currentNode.nextSibling
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                tableOfContents.value = tocEntries
            }
        }
    }

    private fun parseOutlineItem(
        outlineItem: PDOutlineItem,
        parentTitle: String,
        document: PDDocument
    ) : TOCEntry {
        val tocEntries = mutableListOf<TOCEntry>()
        val title = parentTitle + outlineItem.title
        val pageIndex = outlineItem.findDestinationPage(document).structParents

        var child = outlineItem.firstChild
        while (child != null) {
            val tocChild = parseOutlineItem(child, "$title -> ", document)
            tocEntries.add(tocChild)
            child = child.nextSibling
        }

        return TOCEntry(title, pageIndex, tocEntries)
    }
}

data class TOCEntry(
    val title: String,
    val pageIndex: Int,
    val children: List<TOCEntry>
)

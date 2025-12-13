package com.example.bottomapp.shared.renderHelper

import androidx.compose.ui.graphics.ImageBitmap
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.ui.PdfBookContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageTree
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO


actual class PdfRendererHelper actual constructor(val pdfFile: suspend () -> FileWrapper) : IRenderHelper<PdfBookContent> {
    val pdfRenderer = MutableStateFlow<PDFRenderer?>(null)
    actual val pagesCount: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val bitmapCache =
        object : LinkedHashMap<bitmapKey, MutableStateFlow<PdfBookContent>>(10, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<bitmapKey, MutableStateFlow<PdfBookContent>>?): Boolean {
                return size > 10
            }
        }
    private val lock = Any()
    private val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val parcelFileDescriptor = pdfFile().file;
            val doc = Loader.loadPDF(parcelFileDescriptor)
            pdfRenderer.value = PDFRenderer(doc)
            pagesCount.update { doc.pages.count }
            extractTableOfContents()
        }
    }

    override fun openPage(
        pageIndex: Int,
        applyInversion: Boolean
    ): StateFlow<PdfBookContent> {
        bitmapCache[bitmapKey(applyInversion, pageIndex)]?.let {
            return it
        }
        val bitmapStateFlow = MutableStateFlow<PdfBookContent>(PdfBookContent(null))
        bitmapCache[bitmapKey(applyInversion, pageIndex)] = bitmapStateFlow
        CoroutineScope(Dispatchers.IO).launch {
            //Log.d("PdfRendererHelper", "openPage: $pageIndex")
            var bitmap: ImageBitmap?
            synchronized(lock) {
                val page = pdfRenderer.value?.renderImageWithDPI(pageIndex, 300f)
                val baos = ByteArrayOutputStream()
                ImageIO.write(page, "png", baos) // Use PNG or JPEG for compression
                val imageBytes = baos.toByteArray()
                bitmap = imageBytes.decodeToImageBitmap()

            }
            withContext(Dispatchers.Main) {
                bitmapStateFlow.value = PdfBookContent(bitmap!!)
            }
        }
        return bitmapStateFlow
    }

    override fun close() {
        synchronized(lock) {
            bitmapCache.clear()
        }
    }

    override fun getBookType(): FormatType {
        return FormatType.PDF
    }

    override fun getTableOfContents(): StateFlow<List<TOCEntry>> = tableOfContents

    private fun extractTableOfContents() {
        CoroutineScope(Dispatchers.IO).launch {
            val tocEntries = mutableListOf<TOCEntry>()
            try {
                val file = pdfFile().file
                val doc = Loader.loadPDF(file)
                doc.use { document ->
                    val catalog = document.documentCatalog
                    val outline = catalog.documentOutline
                    val tree = document.pages
                    if (outline != null) {
                        var currentNode = outline.firstChild
                        while (currentNode != null) {
                            val chapter = parseOutlineItem(currentNode, document, tree)
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
        document: PDDocument,
        tree: PDPageTree
    ): TOCEntry {
        val tocEntries = mutableListOf<TOCEntry>()
        val title = outlineItem.title
        val pageIndex = tree.indexOf(outlineItem.findDestinationPage(document))

        var child = outlineItem.firstChild
        while (child != null) {
            val tocChild = parseOutlineItem(child, document, tree)
            tocEntries.add(tocChild)
            child = child.nextSibling
        }

        return TOCEntry(title, pageIndex, tocEntries)
    }
}
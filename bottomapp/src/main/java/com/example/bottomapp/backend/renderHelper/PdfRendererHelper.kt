package com.example.bottomapp.backend.renderHelper

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.ui.readerscreens.PdfBookContent
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageTree
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererHelper(val pdfFile: suspend () -> File) : IRenderHelper<PdfBookContent> {
    val pdfRenderer = MutableStateFlow<PdfRenderer?>(null)
    val pagesCount: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val bitmapCache = object : LinkedHashMap<bitmapKey, MutableStateFlow<PdfBookContent>>(10, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<bitmapKey, MutableStateFlow<PdfBookContent>>?): Boolean {
            return size > 10
        }
    }
    private val lock = Any()
    private val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val parcelFileDescriptor =
                ParcelFileDescriptor.open(pdfFile(), ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer.value = PdfRenderer(parcelFileDescriptor)
            pagesCount.update { pdfRenderer.value!!.pageCount }
            extractTableOfContents()
        }
    }

    override fun openPage(pageIndex: Int, applyInversion: Boolean): MutableStateFlow<PdfBookContent> {
        bitmapCache[bitmapKey(applyInversion, pageIndex)]?.let {
            return it
        }
        val bitmapStateFlow = MutableStateFlow<PdfBookContent>(PdfBookContent(null))
        bitmapCache[bitmapKey(applyInversion, pageIndex)] = bitmapStateFlow
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PdfRendererHelper", "openPage: $pageIndex")
            var bitmap: Bitmap?
            synchronized(lock) {
                val page = pdfRenderer.value?.openPage(pageIndex)
                bitmap =
                    createBitmap(page!!.width * 5, page.height * 5)
                val rect = Rect(0, 0, page.width * 5, page.height * 5)
                page.render(bitmap!!, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = renderBitmap(
                    bitmap!!.width,
                    bitmap!!.height,
                    applyInversion,
                    bitmap!!
                )
            }
            withContext(Dispatchers.Main) {
                bitmapStateFlow.value = PdfBookContent(bitmap)
            }
        }
        return bitmapStateFlow
    }

    private fun renderBitmap(
        width: Int,
        height: Int,
        applyInversion: Boolean,
        bitmap: Bitmap
    ): Bitmap {
        return createBitmap(width, height).apply {
            val cacheCanvas = android.graphics.Canvas(this)

            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val scaleFactor = 1f * minOf(
                width.toFloat() / bitmapWidth,
                height.toFloat() / bitmapHeight
            )
            val scaledWidth = (bitmapWidth * scaleFactor).toInt()
            val scaledHeight = (bitmapHeight * scaleFactor).toInt()
            val left = ((width - scaledWidth) / 2)
            val top = ((height - scaledHeight) / 2)

            val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)

            cacheCanvas.drawColor(if (applyInversion) Color.Black.toArgb() else Color.White.toArgb())

            if (applyInversion) {
                val colorMatrix = ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                val paint = android.graphics.Paint().apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }
                cacheCanvas.drawBitmap(bitmap, null, destRect, paint)
            } else {
                cacheCanvas.drawBitmap(bitmap, null, destRect, null)
            }
        }
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

    override fun getTableOfContents(): StateFlow<List<TOCEntry>> = tableOfContents

    private fun extractTableOfContents() {
        CoroutineScope(Dispatchers.IO).launch {
            val tocEntries = mutableListOf<TOCEntry>()
            try {
                val file = pdfFile()
                PDDocument.load(file).use { document ->
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
    ) : TOCEntry {
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

data class TOCEntry(
    val title: String,
    val pageIndex: Int,
    val children: List<TOCEntry>
)

data class bitmapKey(
    val applyInversion: Boolean,
    val pageIndex: Int
)
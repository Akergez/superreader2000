package com.example.bottomapp.shared.formats.djvu

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.IRenderHelper
import com.example.bottomapp.shared.formats.pdf.TOCEntry
import com.example.bottomapp.shared.formats.pdf.bitmapKey
import com.example.bottomapp.shared.ui.DjvuBookContent
import com.github.axet.djvulibre.DjvuLibre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

actual class DJVURenderHelper(val pdfFile: suspend () -> File) : IRenderHelper<DjvuBookContent> {
    private val tableOfContents = MutableStateFlow<List<TOCEntry>>(emptyList())
    private val _pagesCount = MutableStateFlow<Int?>(null)
    val pagesCount: StateFlow<Int?> = _pagesCount
    private val bitmapCache =
        object : LinkedHashMap<bitmapKey, MutableStateFlow<DjvuBookContent>>(10, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<bitmapKey, MutableStateFlow<DjvuBookContent>>?): Boolean {
                return size > 10
            }
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            extractTableOfContents()
            val descriptor = FileInputStream(pdfFile())
            descriptor.fd.sync()
            val libre = DjvuLibre(descriptor.fd)
            _pagesCount.value = libre.pagesCount
        }
    }

    override fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<DjvuBookContent> {
        if (bitmapCache.contains(bitmapKey(applyInversion, pageIndex))) {
            return bitmapCache[bitmapKey(applyInversion, pageIndex)]!!
        }
        val msf = MutableStateFlow(DjvuBookContent(null))
        bitmapCache[bitmapKey(applyInversion, pageIndex)] = msf
        var bitmap: Bitmap?
        CoroutineScope(Dispatchers.IO).launch {
            val file = pdfFile()
            val descriptor = FileInputStream(file)
            descriptor.fd.sync()
            val libre = DjvuLibre(descriptor.fd)
            val page = libre.getPageInfo(pageIndex)!!
            bitmap = createBitmap(page.width, page.height)
            libre.renderPage(
                bitmap, pageIndex, 0, 0, page.width, page.height, 0, 0, page.width, page.height
            )
            val readyBitmap = renderBitmap(page.width, page.height, applyInversion, bitmap!!)
            bitmap = readyBitmap
            libre.close()
            descriptor.close()

            withContext(Dispatchers.Main) {
                msf.value = DjvuBookContent(bitmap.asImageBitmap())
            }
        }
        return msf
    }

    private fun renderBitmap(
        width: Int, height: Int, applyInversion: Boolean, bitmap: Bitmap
    ): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val cacheCanvas = Canvas(this)

            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val scaleFactor = 1f * minOf(
                width.toFloat() / bitmapWidth, height.toFloat() / bitmapHeight
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
                            -1f,
                            0f,
                            0f,
                            0f,
                            255f,
                            0f,
                            -1f,
                            0f,
                            0f,
                            255f,
                            0f,
                            0f,
                            -1f,
                            0f,
                            255f,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f
                        )
                    )
                }
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }
                cacheCanvas.drawBitmap(bitmap, null, destRect, paint)
            } else {
                cacheCanvas.drawBitmap(bitmap, null, destRect, null)
            }
        }
    }


    actual override fun close() {
    }

    actual override fun getBookType(): FormatType {
        return FormatType.DJVU
    }

    actual override fun getTableOfContents(): StateFlow<List<TOCEntry>> = tableOfContents

    private fun extractTableOfContents() {
        CoroutineScope(Dispatchers.IO).launch {
            val file = pdfFile()
            val rawEntries: List<TOCEntry>
            val descriptor = FileInputStream(file)
            descriptor.fd.sync()
            val libre = DjvuLibre(descriptor.fd)
            rawEntries = libre.bookmarks?.map {
                TOCEntry(
                    it.title, it.page, emptyList()
                )
            } ?: emptyList()
            libre.close()
            descriptor.close()
            withContext(Dispatchers.Main) {
                tableOfContents.value = rawEntries
            }
        }
    }
}
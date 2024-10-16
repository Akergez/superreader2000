import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.max

class PdfRendererHelper(private val context: Context, private val pdfFile: File) {
    private val pdfRenderer: PdfRenderer
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    init {
        val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
    }

    fun getPageCount(): Int = pdfRenderer.pageCount

    fun openPage(pageIndex: Int, scale: Float = 4f): Bitmap {
        // Check if the bitmap is already cached
        val cacheKey = pageIndex
        bitmapCache[cacheKey]?.let {
            return it
        }

        // Render the page and cache the bitmap
        val page = pdfRenderer.openPage(pageIndex)
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, width, height)
        page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        // Cache the rendered bitmap
        bitmapCache[cacheKey] = bitmap
        return bitmap
    }

    fun close() {
        pdfRenderer.close()
        bitmapCache.clear()
    }
}
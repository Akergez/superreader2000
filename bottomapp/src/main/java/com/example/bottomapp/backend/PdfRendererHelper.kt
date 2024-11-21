import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.bottomapp.backend.RenderHelper.IRenderHelper
import java.io.File

class PdfRendererHelper(private val context: Context, private val pdfFile: File): IRenderHelper {
    private val pdfRenderer: PdfRenderer
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private val lock = Any()

    init {
        val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
    }

    override fun getPageCount(): Int = pdfRenderer.pageCount

    override fun openPage(pageIndex: Int): Bitmap {
        bitmapCache[pageIndex]?.let {
            return it
        }
        synchronized(lock) {
            // Check if the bitmap is already cached

            // Render the page and cache the bitmap
            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            val rect = Rect(0, 0, page.width, page.height)
            page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Cache the rendered bitmap
            bitmapCache[pageIndex] = bitmap
            return bitmap
        }
    }

    override fun close() {
        synchronized(lock) {
            pdfRenderer.close()
            bitmapCache.clear()
        }
    }
}
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

class PdfRendererHelper(context: Context, pdfFile: File) {
    private val fileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    private val pdfRenderer: PdfRenderer = PdfRenderer(fileDescriptor)
    var currentPage: PdfRenderer.Page? = null

    fun openPage(index: Int): Bitmap {
        currentPage?.close()
        currentPage = pdfRenderer.openPage(index)

        // Render the page as a bitmap
        val bitmap = Bitmap.createBitmap(
            currentPage!!.width, currentPage!!.height,
            Bitmap.Config.ARGB_8888
        )
        currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    fun getPageCount(): Int = pdfRenderer.pageCount

    fun close() {
        currentPage?.close()
        pdfRenderer.close()
        fileDescriptor.close()
    }
}
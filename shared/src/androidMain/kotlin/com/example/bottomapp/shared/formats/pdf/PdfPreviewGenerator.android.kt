package com.example.bottomapp.shared.formats.pdf

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.Image
import java.io.IOException

actual class PdfPreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        var pdfRenderer: PdfRenderer? = null
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(filePath.file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val width = page.width
                val height = page.height
                val bitmap = createBitmap(width, height)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                return Image(bitmap)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }
}

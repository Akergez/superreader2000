package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.Image
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.IOException


actual class PdfPreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        var document: org.apache.pdfbox.pdmodel.PDDocument? = null
        try {
            document = Loader.loadPDF(filePath.file)
            if ((document?.numberOfPages ?: 0) > 0) {
                val pdfRenderer = PDFRenderer(document!!)
                val bufferedImage = pdfRenderer.renderImageWithDPI(0, 72f)
                return Image(bufferedImage)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                document?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }
}


package com.akergez.superreader2000.shared.formats.djvu

import android.graphics.Bitmap
import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.Image
import com.github.axet.djvulibre.DjvuLibre
import java.io.FileInputStream

actual class DjvuPreviewGenerator : IPreviewGenerator {
    actual override suspend fun generatePreview(filePath: FileWrapper): Image? {
        val render = DjvuLibre(FileInputStream(filePath.file).fd)
        val page = render.getPageInfo(0)
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        render.renderPage(
            bitmap,
            0,
            0,
            0,
            page.width,
            page.height,
            0,
            0,
            page.width,
            page.height
        )
        return Image(bitmap);
    }

}
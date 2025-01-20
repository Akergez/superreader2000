package com.example.bottomapp.backend.previewGenerators

import android.graphics.Bitmap
import com.github.axet.djvulibre.DjvuLibre
import java.io.File
import java.io.FileInputStream

class DjvuPreviewGenerator : IPreviewGenerator {
    override fun generatePreviewBitmap(file: File): Bitmap {
        val render = DjvuLibre(FileInputStream(file).fd)
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
        return bitmap
    }

}
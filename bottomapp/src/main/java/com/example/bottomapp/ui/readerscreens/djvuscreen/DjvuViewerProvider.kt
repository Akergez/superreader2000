package com.example.bottomapp.ui.readerscreens.djvuscreen

import androidx.compose.runtime.Composable
import com.example.bottomapp.ui.readerscreens.DjvuBookContent
import com.example.bottomapp.ui.readerscreens.common.IViewerProvider

class DjvuViewerProvider : IViewerProvider<DjvuBookContent> {
    override fun getViewer(
        bookContent: DjvuBookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {
        return { DjvuViewer(bookContent.bitmap, applyInversion = applyInversion) }
    }
}
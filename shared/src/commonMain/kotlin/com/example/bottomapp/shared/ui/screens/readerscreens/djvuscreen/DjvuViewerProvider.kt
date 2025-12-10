package com.example.bottomapp.shared.ui.screens.readerscreens.djvuscreen

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.ui.DjvuBookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.IViewerProvider

class DjvuViewerProvider : IViewerProvider<DjvuBookContent> {
    override fun getViewer(
        bookContent: DjvuBookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {
        return { DjvuViewer(bookContent.bitmap, applyInversion = applyInversion) }
    }
}
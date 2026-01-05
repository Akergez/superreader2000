package com.akergez.superreader2000.shared.formats.djvu.ui

import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.ui.DjvuBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider

class DjvuViewerProvider : IViewerProvider<DjvuBookContent> {
    override fun getViewer(
        bookContent: DjvuBookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {
        return { DjvuViewer(bookContent.bitmap, applyInversion = applyInversion) }
    }
}
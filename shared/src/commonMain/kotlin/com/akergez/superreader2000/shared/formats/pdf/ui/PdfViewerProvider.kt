package com.akergez.superreader2000.shared.formats.pdf.ui

import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.formats.pdf.ui.components.PdfViewer
import com.akergez.superreader2000.shared.ui.PdfBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider

class PdfViewerProvider : IViewerProvider<PdfBookContent> {
    override fun getViewer(
        bookContent: PdfBookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {
        return { PdfViewer(bitmap = bookContent.bitmap, applyInversion = false) }
    }
}
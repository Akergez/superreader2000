package com.example.bottomapp.shared.ui.screens.readerscreens.pdfscreen

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.ui.PdfBookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.IViewerProvider

class PdfViewerProvider : IViewerProvider<PdfBookContent> {
    override fun getViewer(
        bookContent: PdfBookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {
        return { PdfViewer(bitmap = bookContent.bitmap, applyInversion = false) }
    }
}
package com.example.bottomapp.ui.readerscreens.pdfscreen

import androidx.compose.runtime.Composable
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.PdfBookContent
import com.example.bottomapp.ui.readerscreens.common.IViewerProvider

class PdfViewerProvider : IViewerProvider<PdfBookContent> {
    override fun getViewer(
        bookContent: PdfBookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {
        return { PdfViewer(bitmap = bookContent.bitmap, applyInversion = false) }
    }
}
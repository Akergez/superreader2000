package com.example.bottomapp.ui.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.backend.RenderHelper.IRenderHelper
import com.example.bottomapp.ui.readerscreens.fb2screen.Fb2Viewer
import com.example.bottomapp.ui.readerscreens.pdfscreen.CurlPdfViewer
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfViewer

@Composable
fun BookPage(
    page: Int,
    renderHelper: IRenderHelper,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    async: Boolean = true
) {
    Box(
        modifier = modifier
    ) {
        if (renderHelper.getBookType() == FormatType.PDF) {
            if (async) {
                PdfViewer(
                    renderHelper = renderHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            } else {
                CurlPdfViewer(
                    renderHelper = renderHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        } else {
            Fb2Viewer(
                renderHelper = renderHelper,
                pageIndex = page,
                applyInversion = applyInversion, // Pass the inversion setting
                scale = scale, // Pass the zoom scale,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }
    }
}

@Composable
fun Modifier.basePageBoxModifier(): Modifier {
    return this
        .padding(8.dp)
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)
}

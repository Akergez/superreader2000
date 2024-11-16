package com.example.bottomapp.ui.readerscreens.pdfscreen

import PdfRendererHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PdfPage(
    page: Int,
    pdfRendererHelper: PdfRendererHelper,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    scale : Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
) {
    Box(
        modifier = modifier
    ) {
        // Enable pinch-to-zoom
        PdfViewer(
            pdfRendererHelper = pdfRendererHelper,
            pageIndex = page,
            applyInversion = applyInversion, // Pass the inversion setting
            scale = scale, // Pass the zoom scale,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }
}

@Composable
fun Modifier.basePageBoxModifier(): Modifier {
    return this
        .padding(8.dp)
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)
}

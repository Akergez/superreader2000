package com.example.bottomapp.ui.readerscreens.pdfscreen

import CanvasWithPage
import PdfRendererHelper
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfViewer(
    pdfRendererHelper: PdfRendererHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = if (applyInversion) Color.Black else Color.White

    // Offload PDF rendering to a background thread
    LaunchedEffect(pageIndex) {
        coroutineScope.launch(Dispatchers.IO) {
            val renderedBitmap = pdfRendererHelper.openPage(pageIndex)
            withContext(Dispatchers.Main) {
                bitmap = renderedBitmap
            }
        }
    }

    // Only render when bitmap is ready
    bitmap?.let { loadedBitmap ->
        CanvasWithPage(
            modifier = modifier,
            bitmap = loadedBitmap,
            scale = scale.toDouble(),
            offsetX = offsetX.toDouble(),
            offsetY = offsetY.toDouble(),
            backgroundColor = backgroundColor,
            applyInversion = applyInversion
        )
    } ?: run {
        // Show loading indicator while page is being rendered
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", color = if (applyInversion) Color.White else Color.Black)
        }
    }
}

@Composable
fun CurlPdfViewer(
    pdfRendererHelper: PdfRendererHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val bitmap = pdfRendererHelper.openPage(pageIndex)
    val backgroundColor = if (applyInversion) Color.Black else Color.White

    CanvasWithPage(
        modifier,
        bitmap,
        scale.toDouble(),
        offsetX.toDouble(),
        offsetY.toDouble(),
        backgroundColor,
        applyInversion
    )
}
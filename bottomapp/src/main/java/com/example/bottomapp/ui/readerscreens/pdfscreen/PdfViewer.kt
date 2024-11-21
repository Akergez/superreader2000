package com.example.bottomapp.ui.readerscreens.pdfscreen

import com.example.bottomapp.ui.readerscreens.common.BitmapCanvas
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
import com.example.bottomapp.backend.RenderHelper.IRenderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfViewer(
    renderHelper: IRenderHelper,
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

    // Offload page rendering to a background thread
    LaunchedEffect(pageIndex) {
        coroutineScope.launch(Dispatchers.IO) {
            val renderedBitmap = renderHelper.openPage(pageIndex)
            withContext(Dispatchers.Main) {
                bitmap = renderedBitmap
            }
        }
    }

    // Only render when bitmap is ready
    bitmap?.let { loadedBitmap ->
        BitmapCanvas(
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
    renderHelper: IRenderHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val bitmap = renderHelper.openPage(pageIndex)
    val backgroundColor = if (applyInversion) Color.Black else Color.White

    BitmapCanvas(
        modifier,
        bitmap,
        scale.toDouble(),
        offsetX.toDouble(),
        offsetY.toDouble(),
        backgroundColor,
        applyInversion
    )
}
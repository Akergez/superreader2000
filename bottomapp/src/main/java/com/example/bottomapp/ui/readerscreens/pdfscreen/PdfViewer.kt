package com.example.bottomapp.ui.readerscreens.pdfscreen

import com.example.bottomapp.ui.readerscreens.common.BitmapCanvas
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R

@Composable
fun PdfViewer(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val backgroundColor = if (applyInversion) Color.Black else Color.White
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
            Text(getString(LocalContext.current, R.string.loading), color = if (applyInversion) Color.White else Color.Black)
        }
    }
}
package com.akergez.superreader2000.shared.formats.djvu.ui

import BitmapCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun DjvuViewer(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    applyInversion: Boolean
) {
    val backgroundColor = if (applyInversion) Color.Black else Color.White
    bitmap?.let { loadedBitmap ->
        BitmapCanvas(
            modifier = modifier,
            bitmap = loadedBitmap
        )
    } ?: run {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

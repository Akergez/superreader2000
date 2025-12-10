package com.example.bottomapp.shared.ui.screens.readerscreens.djvuscreen

import BitmapCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
        // Show loading indicator while page is being rendered
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            //Text(getString(LocalContext.current, R.string.loading), color = if (applyInversion) Color.White else Color.Black)
        }
    }
}
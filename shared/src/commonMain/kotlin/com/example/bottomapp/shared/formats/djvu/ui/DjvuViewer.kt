package com.example.bottomapp.shared.formats.djvu.ui

import BitmapCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.stringResource
import pdfreaderjetpackcompose.shared.generated.resources.Res
import pdfreaderjetpackcompose.shared.generated.resources.loading

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
            Text(
                stringResource(Res.string.loading),
                color = if (applyInversion) Color.White else Color.Black
            )
        }
    }
}
package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import com.example.bottomapp.ui.readerscreens.common.TransformState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun Zoom(
    currentPage: Int,
    getPage: (Int) -> StateFlow<IBookContent>,
    documentTheme: DocThemeMode,
    modifier: Modifier,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onZoomLock: () -> Unit
) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = onZoomLock) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock zoom")
        }
    }) { inner ->
        BookPage(
            bookContent = getPage(currentPage),
            applyInversion = documentTheme == DocThemeMode.Invert,
            modifier = modifier
                .padding(inner)
                .fillMaxSize()
                .consumeWindowInsets(inner),
            transformState = TransformState(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            )
        )
    }
}
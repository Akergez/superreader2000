package com.example.bottomapp.ui.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.readerscreens.Fb2BookContent
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.PdfBookContent
import com.example.bottomapp.ui.readerscreens.fb2screen.Fb2Viewer
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfViewer
import kotlinx.coroutines.flow.StateFlow

data class TransformState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

@Composable
fun BookPage(
    bookContent: StateFlow<IBookContent>,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    transformState: TransformState = TransformState()
) {
    val content by bookContent.collectAsState()
    Box(modifier = modifier) {
        when (content) {
            is PdfBookContent -> {
                val pdfContent = content as PdfBookContent
                PdfViewer(
                    applyInversion = applyInversion,
                    scale = transformState.scale,
                    offsetX = transformState.offsetX,
                    offsetY = transformState.offsetY,
                    bitmap = pdfContent.bitmap
                )
            }

            is Fb2BookContent -> {
                val fb2BookContent = content as Fb2BookContent
                Fb2Viewer(
                    content = fb2BookContent,
                    applyInversion = applyInversion,
                    scale = transformState.scale,
                    offsetX = transformState.offsetX,
                    offsetY = transformState.offsetY
                )
            }
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

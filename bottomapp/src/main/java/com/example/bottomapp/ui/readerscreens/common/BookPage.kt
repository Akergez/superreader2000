package com.example.bottomapp.ui.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.bottomapp.ui.readerscreens.Fb2BookContent
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.PdfBookContent
import com.example.bottomapp.ui.readerscreens.fb2screen.Fb2Viewer
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfViewer
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BookPage(
    bookContent: StateFlow<IBookContent>,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
) {
    val content by bookContent.collectAsState()
    Box(modifier.background(color = if (applyInversion) Color.Black else Color.White)) {
        when (content) {
            is PdfBookContent -> {
                val pdfContent = content as PdfBookContent
                PdfViewer(
                    bitmap = pdfContent.bitmap,
                    applyInversion = applyInversion
                )
            }

            is Fb2BookContent -> {
                val fb2BookContent = content as Fb2BookContent
                Fb2Viewer(
                    content = fb2BookContent,
                    applyInversion = applyInversion,
                    scale = 1f,
                    offsetX = 0f,
                    offsetY = 0f
                )
            }
        }
    }
}
package com.example.bottomapp.ui.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.bottomapp.ui.readerscreens.IBookContent
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <TBookContentType>BookPage(
    bookContent: StateFlow<TBookContentType>,
    viewerProvider: IViewerProvider<TBookContentType>,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
) where TBookContentType : IBookContent {
    val content by bookContent.collectAsState()
    Box(modifier.background(color = if (applyInversion) Color.Black else Color.White)) {
        viewerProvider.getViewer(bookContent = content, applyInversion = applyInversion)()
    }
}
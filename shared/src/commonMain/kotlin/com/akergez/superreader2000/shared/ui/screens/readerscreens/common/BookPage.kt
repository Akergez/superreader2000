package com.akergez.superreader2000.shared.ui.screens.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.akergez.superreader2000.shared.ui.IBookContent
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <TBookContentType>BookPage(
    bookContent: StateFlow<TBookContentType>,
    viewerProvider: IViewerProvider<TBookContentType>,
    applyInversion: Boolean,
    backgroundColor: Color = if (applyInversion) Color.Black else Color.White,
    modifier: Modifier = Modifier,
) where TBookContentType : IBookContent {
    val content by bookContent.collectAsState()
    Box(modifier.background(backgroundColor)) {
        viewerProvider.getViewer(bookContent = content, applyInversion = applyInversion)()
    }
}

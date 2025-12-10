package com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.ui.IBookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.IViewerProvider
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.ZoomState

@Composable
actual fun <TBookContent : IBookContent> PageCurl(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    currentPage: Int,
    getPage: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    documentTheme: DocThemeMode,
    modifier: Modifier,
    zoomState: (ZoomState) -> Unit
) {

}
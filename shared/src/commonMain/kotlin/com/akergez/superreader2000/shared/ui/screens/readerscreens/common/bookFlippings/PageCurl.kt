package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.IBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.ZoomState

@Composable
expect fun <TBookContent> PageCurl(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    currentPage: Int,
    getPage: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    documentTheme: DocThemeMode,
    modifier: Modifier = Modifier,
    zoomState: (ZoomState) -> Unit
) where TBookContent : IBookContent
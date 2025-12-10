package com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings

import android.util.Log
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.ui.IBookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.BookPage
import com.example.bottomapp.shared.ui.screens.readerscreens.common.IViewerProvider
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurlState
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalPageCurlApi::class)
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

    //Log.d("BookContent", "currentPage = $currentPage")
    val pagerState = remember { PageCurlState(initialCurrent = currentPage, initialMax = 0) }
    val totalPagesState by totalPages.collectAsState()
    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }

    val zoomS = rememberZoomState()
    LaunchedEffect(zoomS.scale) { zoomState(zoomS) }

    Scaffold { innerPadding ->
        totalPagesState?.let {
            eu.wewox.pagecurl.page.PageCurl(
                count = it,
                state = pagerState,
                config = rememberPageCurlConfig(
                    backPageContentAlpha = 0.5f, backPageColor = Color.Black
                ),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .systemBarsPadding()
                    .zoomable(zoomS)
            ) { index ->
                BookPage(
                    bookContent = getPage(index),
                    applyInversion = documentTheme == DocThemeMode.Invert,
                    modifier = modifier,
                    viewerProvider = viewerProvider
                )
            }
        }
    }
}
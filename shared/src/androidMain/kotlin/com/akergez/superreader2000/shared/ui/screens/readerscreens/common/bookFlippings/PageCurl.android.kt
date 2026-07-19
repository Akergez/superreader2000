package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.IBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.BookPage
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider
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
    val totalPagesState by totalPages.collectAsState()
    val zoomS = rememberZoomState()
    LaunchedEffect(zoomS.scale) { zoomState(zoomS) }
    val backgroundColor = when (documentTheme) {
        DocThemeMode.AsIs -> Color.White
        DocThemeMode.Invert -> Color.Black
        DocThemeMode.SoftLight -> Color(0xFFD6D3CC)
        DocThemeMode.SoftDark -> Color(0xFF242424)
        DocThemeMode.MaterialYou -> MaterialTheme.colorScheme.surface
    }

    Scaffold(
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        totalPagesState?.takeIf { it > 0 }?.let { pageCount ->
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val isLandscape = constraints.maxWidth.toFloat() / constraints.maxHeight > 4f / 3f
                val pagesPerSpread = if (isLandscape) 2 else 1
                val spreadCount = (pageCount + pagesPerSpread - 1) / pagesPerSpread
                val initialSpread = (currentPage / pagesPerSpread).coerceIn(0, spreadCount - 1)
                val pagerState = remember(pagesPerSpread) {
                    PageCurlState(initialCurrent = initialSpread, initialMax = 0)
                }

                LaunchedEffect(pagerState.current, pagesPerSpread) {
                    onPageChange(pagerState.current * pagesPerSpread)
                }

                eu.wewox.pagecurl.page.PageCurl(
                    count = spreadCount,
                    state = pagerState,
                    config = rememberPageCurlConfig(
                        backPageContentAlpha = 0.5f, backPageColor = backgroundColor
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(zoomS)
                ) { spreadIndex ->
                    val leftPage = spreadIndex * pagesPerSpread
                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            val leftState = remember(leftPage, getPage) { getPage(leftPage) }
                            BookPage(
                                bookContent = leftState,
                                applyInversion = documentTheme == DocThemeMode.Invert,
                                backgroundColor = backgroundColor,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                viewerProvider = viewerProvider,
                            )

                            val rightPage = leftPage + 1
                            if (rightPage < pageCount) {
                                val rightState = remember(rightPage, getPage) { getPage(rightPage) }
                                BookPage(
                                    bookContent = rightState,
                                    applyInversion = documentTheme == DocThemeMode.Invert,
                                    backgroundColor = backgroundColor,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    viewerProvider = viewerProvider,
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(backgroundColor))
                            }
                        }
                    } else {
                        val pageState = remember(leftPage, getPage) { getPage(leftPage) }
                        BookPage(
                            bookContent = pageState,
                            applyInversion = documentTheme == DocThemeMode.Invert,
                            backgroundColor = backgroundColor,
                            modifier = Modifier.fillMaxSize(),
                            viewerProvider = viewerProvider,
                        )
                    }
                }
            }
        }
    }
}

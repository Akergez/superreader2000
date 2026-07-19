package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akergez.superreader2000.shared.ui.IBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.BookPage
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun <TBookContent> CasualCurl(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    getPage: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    zoomState: (ZoomState) -> Unit
) where TBookContent : IBookContent {
    val totalPagesState by totalPages.collectAsState()
    val zoomS = rememberZoomState()
    LaunchedEffect(zoomS.scale) { zoomState(zoomS) }

    Scaffold(
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        totalPagesState?.takeIf { it > 0 }?.let { totalPagesValue ->
            BoxWithConstraints(
                modifier = modifier.padding(innerPadding).fillMaxSize()
            ) {
                val isLandscape = constraints.maxWidth.toFloat() / constraints.maxHeight > 4f / 3f
                val pagesPerSpread = if (isLandscape) 2 else 1
                val spreadCount = (totalPagesValue + pagesPerSpread - 1) / pagesPerSpread
                val initialSpread = (initialPage / pagesPerSpread).coerceIn(0, spreadCount - 1)

                var lastBookPage by remember { mutableIntStateOf(initialPage) }
                val pagerState = rememberPagerState(initialSpread) { spreadCount }

                // Синхронизируем внутреннее состояние pager с внешней позицией чтения.
                LaunchedEffect(initialPage, pagesPerSpread, spreadCount) {
                    pagerState.scrollToPage(
                        (initialPage / pagesPerSpread).coerceIn(0, spreadCount - 1)
                    )
                    lastBookPage = initialPage
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize().zoomable(zoomS),
                    state = pagerState,
                    key = { it }
                ) { spreadIndex ->
                    val leftPage = spreadIndex * pagesPerSpread
                    val rightPage = if (isLandscape && leftPage + 1 < totalPagesValue) leftPage + 1 else null

                    Card(
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (rightPage != null) {
                            // Two pages side by side on one card — each column gets half width.
                            Row(modifier = Modifier.fillMaxSize()) {
                                val leftState = remember(leftPage, getPage) { getPage(leftPage) }
                                BookPage(
                                    bookContent = leftState,
                                    applyInversion = applyInversion,
                                    backgroundColor = backgroundColor,
                                    viewerProvider = viewerProvider,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                val rightState = remember(rightPage, getPage) { getPage(rightPage) }
                                BookPage(
                                    bookContent = rightState,
                                    applyInversion = applyInversion,
                                    backgroundColor = backgroundColor,
                                    viewerProvider = viewerProvider,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        } else {
                            val pageState = remember(leftPage, getPage) { getPage(leftPage) }
                            BookPage(
                                bookContent = pageState,
                                applyInversion = applyInversion,
                                backgroundColor = backgroundColor,
                                viewerProvider = viewerProvider,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                val currentSpread by remember { derivedStateOf { pagerState.currentPage } }
                LaunchedEffect(currentSpread) {
                    val bookPage = currentSpread * pagesPerSpread
                    lastBookPage = bookPage
                    onPageChange(bookPage)
                }
            }
        }
    }
}

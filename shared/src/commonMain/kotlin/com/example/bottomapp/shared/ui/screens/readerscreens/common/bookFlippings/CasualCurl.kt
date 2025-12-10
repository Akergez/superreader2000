package com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.shared.ui.IBookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.BookPage
import com.example.bottomapp.shared.ui.screens.readerscreens.common.IViewerProvider
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun <TBookContent>CasualCurl(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    getPage: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    zoomState: (ZoomState) -> Unit
) where TBookContent : IBookContent {
    val totalPagesState by totalPages.collectAsState()
    val lazyListState = rememberPagerState(initialPage, pageCount = { totalPagesState ?: 0 })
    val zoomS = rememberZoomState()
    LaunchedEffect(zoomS.scale) { zoomState(zoomS) }

    Scaffold { innerPadding ->
        totalPagesState?.let { totalPagesValue ->
            HorizontalPager(
                modifier = modifier.padding(innerPadding).fillMaxSize().zoomable(zoomS),
                state = lazyListState,
                key = { it }) { page ->
                Card(shape = RoundedCornerShape(0.dp), modifier = Modifier.fillMaxSize()) {
                    val pageState = remember { getPage(page) }
                    BookPage(
                        bookContent = pageState,
                        applyInversion = applyInversion,
                        viewerProvider = viewerProvider
                    )
                }

            }

            val firstVisibleItemIndex by remember {
                derivedStateOf { lazyListState.currentPage }
            }

            LaunchedEffect(firstVisibleItemIndex) {
                onPageChange(firstVisibleItemIndex)
            }

        }
    }
}
package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import com.example.bottomapp.ui.readerscreens.common.IViewerProvider
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun <TBookContent>VerticalBookScroll(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    getPage: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    zoomState: (ZoomState) -> Unit
) where TBookContent : IBookContent {
    val lazyListState = rememberLazyListState(initialPage)
    val totalPagesState by totalPages.collectAsState()
    val zoomS = rememberZoomState()
    LaunchedEffect(zoomS.scale) { zoomState(zoomS) }


    Scaffold { innerPadding ->
        totalPagesState?.let { totalPagesValue ->
            Column(modifier = modifier.padding(innerPadding).zoomable(zoomS)) {
                LazyColumn(
                    state = lazyListState,
                ) {
                    items(totalPagesValue, key = { it }) { page ->
                        Box(
                            Modifier
                                .padding(vertical = 5.dp)
                        ) {
                            Card(
                                Modifier
                                    .fillParentMaxHeight()
                            ) {
                                BookPage(
                                    bookContent = getPage(page),
                                    applyInversion = applyInversion,
                                    viewerProvider = viewerProvider
                                )
                            }
                        }
                    }
                }

                // Emit page change when the page is scrolled
                val firstVisibleItemIndex by remember {
                    derivedStateOf { lazyListState.firstVisibleItemIndex }
                }

                // Synchronize the page change
                LaunchedEffect(firstVisibleItemIndex) {
                    onPageChange(firstVisibleItemIndex)
                }

            }
        }
    }
}
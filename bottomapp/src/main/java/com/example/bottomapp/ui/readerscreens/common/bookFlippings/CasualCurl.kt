package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import com.example.bottomapp.ui.readerscreens.common.TransformState
import com.example.bottomapp.ui.readerscreens.common.detectCustomTransformGestures
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPagerApi::class)
@Composable
fun CasualCurl(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    getPage: (Int) -> StateFlow<IBookContent>,
    persistentZoom: TransformState,
    transformGestureHandler: (Offset, Offset, Float, Float, PointerInputChange, List<PointerInputChange>) -> Unit
) {
    val lazyListState = rememberPagerState(initialPage)
    val totalPagesState by totalPages.collectAsState()

    Scaffold { innerPadding ->
        totalPagesState?.let { totalPagesValue ->
            HorizontalPager(
                modifier = modifier.padding(innerPadding).fillMaxSize(),
                state = lazyListState,
                count = totalPagesValue,
                key = { it }) { page ->
                Card(shape = RoundedCornerShape(0.dp), modifier = Modifier.fillMaxSize()) {
                    val pageState = remember { getPage(page) }
                    BookPage(
                        transformState = persistentZoom,
                        applyInversion = applyInversion,
                        bookContent = pageState,
                        modifier = Modifier.pointerInput(Unit) {
                            detectCustomTransformGestures(
                                onGesture = transformGestureHandler,
                                consume = false
                            )
                        },
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
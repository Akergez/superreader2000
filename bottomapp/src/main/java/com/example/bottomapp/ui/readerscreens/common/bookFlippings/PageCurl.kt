package com.example.bottomapp.ui.readerscreens.common.bookFlippings

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import com.example.bottomapp.ui.readerscreens.common.TransformState
import com.example.bottomapp.ui.readerscreens.common.basePageBoxModifier
import com.example.bottomapp.ui.readerscreens.common.detectCustomTransformGestures
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurlState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PageCurl(
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    currentPage: Int,
    getPage: (Int) -> StateFlow<IBookContent>,
    documentTheme: DocThemeMode,
    modifier: Modifier = Modifier,
    persistentZoom: TransformState,
    transformGestureHandler: (Offset, Offset, Float, Float, PointerInputChange, List<PointerInputChange>) -> Unit
) {
    Log.d("BookContent", "currentPage = $currentPage")
    val pagerState = remember { PageCurlState(initialCurrent = currentPage, initialMax = 0) }
    val totalPagesState by totalPages.collectAsState()
    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }
    Scaffold { innerPadding ->
        totalPagesState?.let {
            eu.wewox.pagecurl.page.PageCurl(
                count = it,
                state = pagerState,
                config = rememberPageCurlConfig(
                    backPageContentAlpha = 0.5f,
                    backPageColor = Color.Black
                ),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .systemBarsPadding()
            ) { index ->
                BookPage(
                    transformState = persistentZoom,
                    bookContent = getPage(index),
                    applyInversion = documentTheme == DocThemeMode.Invert,
                    modifier = modifier
                        .basePageBoxModifier()
                        .pointerInput(Unit) {
                            detectCustomTransformGestures(
                                onGesture = transformGestureHandler,
                                consume = false
                            )
                        },
                )
            }
        }
    }
}
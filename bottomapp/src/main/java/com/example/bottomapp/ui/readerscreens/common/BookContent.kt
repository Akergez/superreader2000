package com.example.bottomapp.ui.readerscreens.common

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.PageCarousel
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.VerticalBookScroll
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun BookContent(
    currentPage: Int,
    totalPages: StateFlow<Int>,
    mode: ReaderMode,
    flipMode: FlipMode,
    onPageChange: (Int) -> Unit,
    onToggleMode: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier,
    getPage: (Int) -> StateFlow<IBookContent>,
    documentTheme: DocThemeMode,
    updateDocumentThemeMode: (DocThemeMode) -> Unit,
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)
    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }

    var scale by remember { mutableFloatStateOf(1f) } // State to control zoom scale
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Preload next and previous pages
    LaunchedEffect(currentPage, pagerState.current, totalPages) {
        onPageChange(pagerState.current)
        launch(Dispatchers.IO) {
            Log.e("page", "plus one" + pagerState.current + " " + totalPages)

            if (pagerState.current + 1 < totalPages.value) {
                getPage(pagerState.current + 1)
            }
            if (pagerState.current > 0) {
                getPage(pagerState.current - 1)
            }
        }
    }

    val transformGesturesHandler = { _: Offset, pan: Offset, zoom: Float, _: Float ->
        val newScale = zoom * scale

        if (abs(newScale - 1f) < 0.5f
            || (mode == ReaderMode.Zoom && offsetX + pan.x == 0f)
        ) {
            if (mode != ReaderMode.Reading)
                onToggleMode(ReaderMode.Reading)
        } else if (newScale < 0.5f) {
            if (mode != ReaderMode.Carousel)
                onToggleMode(ReaderMode.Carousel)
        } else {
            if (mode != ReaderMode.Zoom)
                onToggleMode(ReaderMode.Zoom)
        }
        scale = (scale * zoom)
        offsetX += pan.x
        offsetY += pan.y
    }

    Box(modifier = modifier.pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, rotation ->
            transformGesturesHandler(centroid, pan, zoom, rotation)
        }
    })
    {
        when (mode) {
            ReaderMode.Carousel -> {
                PageCarousel(
                    totalPages = totalPages,
                    initialPage = currentPage,
                    onPageChange = onPageChange,
                    applyInversion = documentTheme == DocThemeMode.Invert,
                    onApplyInversionToggle = { applyInversion ->
                        updateDocumentThemeMode(if (applyInversion) DocThemeMode.Invert else DocThemeMode.AsIs)
                    },
                    modifier = modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onToggleMode(ReaderMode.Reading)
                                }
                            )
                        },
                    bookContent = getPage
                )
            }

            ReaderMode.Reading -> {
                when (flipMode) {
                    FlipMode.PagingCurl -> com.example.bottomapp.ui.readerscreens.common.bookFlippings.PageCurl(
                        totalPages = totalPages,
                        currentPage = currentPage,
                        getPage = getPage,
                        documentTheme = documentTheme,
                        modifier = modifier
                    )

                    FlipMode.VerticalScroll -> VerticalBookScroll(
                        totalPages = totalPages,
                        initialPage = currentPage,
                        onPageChange = onPageChange,
                        applyInversion = documentTheme == DocThemeMode.Invert,
                        getPage = getPage
                    )
                }
            }

            ReaderMode.Zoom -> {
                BookPage(
                    bookContent = getPage(currentPage),
                    applyInversion = documentTheme == DocThemeMode.Invert,
                    modifier = modifier
                        .basePageBoxModifier(),
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
    }
}



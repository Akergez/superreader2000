package com.example.bottomapp.ui.readerscreens.common

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
import com.example.bottomapp.backend.RenderHelper.IRenderHelper
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.PageCarousel
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.VerticalBookScroll
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun BookContent(
    currentPage: Int,
    totalPages: Int,
    mode: ReaderMode,
    curlMode: CurlMode,
    renderHelper: IRenderHelper,
    onPageChange: (Int) -> Unit,
    onToggleMode: (ReaderMode) -> Unit,
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)

    var scale by remember { mutableFloatStateOf(1f) } // State to control zoom scale
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Preload next and previous pages
    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
        launch(Dispatchers.IO) {
            if (pagerState.current < totalPages - 1) {
                renderHelper.openPage(pagerState.current + 1)
            }
            if (pagerState.current > 0) {
                renderHelper.openPage(pagerState.current - 1)
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
                    renderHelper = renderHelper,
                    applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                    onApplyInversionToggle = { applyInversion ->
                        settingsManager.setDocumentsTheme(if (applyInversion) DocThemeMode.Invert else DocThemeMode.AsIs)
                    },
                    modifier = modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onToggleMode(ReaderMode.Reading)
                                }
                            )
                        }
                )
            }

            ReaderMode.Reading -> {
                when (curlMode) {
                    CurlMode.PagingCurl -> PageCurl(
                        count = totalPages,
                        state = pagerState,
                        config = rememberPageCurlConfig(
                            backPageContentAlpha = 0f,
                            backPageColor = MaterialTheme.colorScheme.background
                        ),
                    ) { index ->
                        BookPage(
                            page = index,
                            renderHelper = renderHelper,
                            applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                            modifier = modifier
                                .basePageBoxModifier(),
                            async = false
                        )
                    }

                    CurlMode.Vertical -> VerticalBookScroll(
                        totalPages = totalPages,
                        initialPage = currentPage,
                        onPageChange = onPageChange,
                        renderHelper = renderHelper,
                        applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert
                    )
                }
            }

            ReaderMode.Zoom -> {
                BookPage(
                    page = pagerState.current,
                    renderHelper = renderHelper,
                    applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
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



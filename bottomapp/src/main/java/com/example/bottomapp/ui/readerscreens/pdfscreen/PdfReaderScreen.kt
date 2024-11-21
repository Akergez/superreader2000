package com.example.bottomapp.ui.readerscreens.pdfscreen

import com.example.bottomapp.ui.readerscreens.common.BookContent
import com.example.bottomapp.backend.RenderHelper.PdfRendererHelper
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.ReaderMode
import org.koin.compose.koinInject

@Composable
fun PdfReaderScreen(book: PdfBookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }

    val context = LocalContext.current
    val pdfFile = remember { book.getFile() }
    val pdfRendererHelper = remember { PdfRendererHelper(context, pdfFile) }
    val totalPages = remember { pdfRendererHelper.getPageCount() }
    val curlMode = remember { settingsManager.getCurlMode() }

    val onPageChange = { newPage: Int ->
        currentPage = newPage
        book.saveProgress(PdfProgress(newPage))
    }

    Scaffold() { innerPadding ->
        BookContent(
            currentPage = currentPage,
            totalPages = totalPages,
            mode = mode,
            renderHelper = pdfRendererHelper,
            onPageChange = onPageChange,
            onToggleMode = { newMode -> mode = newMode },
            settingsManager = settingsManager,
            curlMode = curlMode,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun BookContent(
    currentPage: Int,
    totalPages: Int,
    mode: ReaderMode,
    curlMode: CurlMode,
    pdfRendererHelper: PdfRendererHelper,
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
                pdfRendererHelper.openPage(pagerState.current + 1)
            }
            if (pagerState.current > 0) {
                pdfRendererHelper.openPage(pagerState.current - 1)
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
                PdfCarousel(
                    totalPages = totalPages,
                    initialPage = currentPage,
                    onPageChange = onPageChange,
                    pdfRendererHelper = pdfRendererHelper,
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
                        PdfPage(
                            page = index,
                            pdfRendererHelper = pdfRendererHelper,
                            applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                            modifier = modifier
                                .basePageBoxModifier(),
                            async = false
                        )
                    }

                    CurlMode.Vertical -> VerticalPdfScroll(
                        totalPages = totalPages,
                        initialPage = currentPage,
                        onPageChange = onPageChange,
                        pdfRendererHelper = pdfRendererHelper,
                        applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert
                    )
                }
            }

            ReaderMode.Zoom -> {
                PdfPage(
                    page = pagerState.current,
                    pdfRendererHelper = pdfRendererHelper,
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



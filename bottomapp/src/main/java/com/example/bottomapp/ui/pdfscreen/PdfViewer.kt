import android.graphics.Bitmap
import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.PageCurlState
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfReaderScreen(book: PdfBookDomainModel, settingsManager: SettingsManager) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(Mode.Curl) }

    val context = LocalContext.current
    val pdfFile = remember { book.getFile() }
    val pdfRendererHelper = remember { PdfRendererHelper(context, pdfFile) }
    val totalPages = remember { pdfRendererHelper.getPageCount() }

    val onPageChange = rememberUpdatedState { newPage: Int ->
        currentPage = newPage
        book.saveProgress(PdfProgress(newPage))
    }

    PdfContent(
        currentPage = currentPage,
        totalPages = totalPages,
        mode = mode,
        pdfRendererHelper = pdfRendererHelper,
        onPageChange = onPageChange.value,
        onToggleMode = { newMode -> mode = newMode },
        settingsManager = settingsManager
    )
}

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun rememberCurlState(
    @IntRange(from = 0) initialPage: Int = 0, max: Int = 0
): PageCurlState = rememberSaveable() {
    PageCurlState(
        initialCurrent = initialPage, initialMax = max
    )
}

enum class Mode {
    Carousel,
    Zoom,
    Curl
}

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PdfContent(
    currentPage: Int,
    totalPages: Int,
    mode: Mode,
    pdfRendererHelper: PdfRendererHelper,
    onPageChange: (Int) -> Unit,
    onToggleMode: (Mode) -> Unit,
    settingsManager: SettingsManager
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)

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

    when (mode) {
        Mode.Carousel -> {
            PdfCarousel(
                totalPages = totalPages,
                initialPage = currentPage,
                onPageChange = onPageChange,
                onToggleMode = onToggleMode,
                pdfRendererHelper = pdfRendererHelper,
                applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                onApplyInversionToggle = { applyInversion ->
                    settingsManager.setDocumentsTheme(if (applyInversion) DocThemeMode.Invert else DocThemeMode.AsIs)
                }
            )
        }

        Mode.Curl -> {
            PageCurl(
                count = totalPages,
                state = pagerState,
                config = rememberPageCurlConfig(
                    backPageContentAlpha = 0f,
                    backPageColor = MaterialTheme.colorScheme.background
                )
            ) { index ->
                PdfPage(
                    page = index,
                    pdfRendererHelper = pdfRendererHelper,
                    applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                    onPageChange = onPageChange,
                    mode = Mode.Curl,
                    onToggleMode = onToggleMode
                )
            }
        }

        Mode.Zoom -> {
            PdfPage(
                page = pagerState.current,
                mode = mode,
                onToggleMode = onToggleMode,
                pdfRendererHelper = pdfRendererHelper,
                onPageChange = onPageChange,
                applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert
            )
        }
    }
}

@Composable
fun PdfCarousel(
    totalPages: Int,
    initialPage: Int = 0,
    onPageChange: (Int) -> Unit,
    onToggleMode: (Mode) -> Unit,
    pdfRendererHelper: PdfRendererHelper,
    applyInversion: Boolean,
    onApplyInversionToggle: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState(initialPage)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.weight(0.9f),
            state = lazyListState,
        ) {
            items(totalPages, key = { it }) { page ->
                Box(
                    Modifier
                        .fillParentMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Card(
                        Modifier
                            .fillParentMaxWidth(0.9f)
                            .align(Alignment.Center)
                    ) {
                        PdfPage(
                            page = page,
                            pdfRendererHelper = pdfRendererHelper,
                            onPageChange = onPageChange,
                            onToggleMode = onToggleMode,
                            mode = Mode.Carousel,
                            applyInversion = applyInversion
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

        // Page indicator and inversion toggle for the carousel
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${firstVisibleItemIndex + 1} of $totalPages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (applyInversion) "Inversion: On" else "Inversion: Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { onApplyInversionToggle(!applyInversion) }
            )
        }
    }
}


@Composable
fun PdfPage(
    page: Int,
    pdfRendererHelper: PdfRendererHelper,
    modifier: Modifier = Modifier,
    onToggleMode: (Mode) -> Unit,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean, // Add this parameter
    mode: Mode,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val haptic = LocalHapticFeedback.current
    var scale by remember { mutableFloatStateOf(1f) } // State to control zoom scale
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isTransforming by remember { mutableStateOf(false) }
    val onZoomModeToggleWrapper = {
        onToggleMode(if (mode == Mode.Zoom) Mode.Curl else Mode.Zoom)
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    when (mode) {
        Mode.Zoom -> {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            isTransforming = true
                            scale = (scale * zoom)
                            offsetX += pan.x
                            offsetY += pan.y
                        }

                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isTransforming) {
                                    onZoomModeToggleWrapper() // Toggle zoom mode on double-click
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onPress = {
                                tryAwaitRelease() // Wait for gestures to finish
                                isTransforming = false // Reset flag after any gesture is done
                            }
                        )
                    }
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                // Enable pinch-to-zoom
                PdfViewer(
                    pdfRendererHelper = pdfRendererHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
        Mode.Carousel -> {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onZoomModeToggleWrapper() // Toggle zoom mode on double-click
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleMode(Mode.Curl)
                            }
                        )
                    }
                    .background(color = MaterialTheme.colorScheme.background)
            ) {

                // Enable pinch-to-zoom
                PdfViewer(
                    pdfRendererHelper = pdfRendererHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onZoomModeToggleWrapper() // Toggle zoom mode on double-click
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleMode(Mode.Carousel)
                            }
                        )
                    }
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                // Enable pinch-to-zoom
                CurlPdfViewer(
                    pdfRendererHelper = pdfRendererHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
    }
}

@Composable
fun PdfViewer(
    pdfRendererHelper: PdfRendererHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = if (applyInversion) Color.Black else Color.White

    // Offload PDF rendering to a background thread
    LaunchedEffect(pageIndex) {
        coroutineScope.launch(Dispatchers.IO) {
            val renderedBitmap = pdfRendererHelper.openPage(pageIndex)
            withContext(Dispatchers.Main) {
                bitmap = renderedBitmap
            }
        }
    }

    // Only render when bitmap is ready
    bitmap?.let { loadedBitmap ->
        CanvasWithPage(
            modifier = modifier,
            bitmap = loadedBitmap,
            scale = scale.toDouble(),
            offsetX = offsetX.toDouble(),
            offsetY = offsetY.toDouble(),
            backgroundColor = backgroundColor,
            applyInversion = applyInversion
        )
    } ?: run {
        // Show loading indicator while page is being rendered
        Box(modifier = modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
            Text("Loading...", color = if (applyInversion) Color.White else Color.Black)
        }
    }
}

@Composable
fun CurlPdfViewer(
    pdfRendererHelper: PdfRendererHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val bitmap = pdfRendererHelper.openPage(pageIndex)
    val backgroundColor = if(applyInversion) Color.Black else Color.White;

    CanvasWithPage(
        modifier,
        bitmap,
        scale.toDouble(),
        offsetX.toDouble(),
        offsetY.toDouble(),
        backgroundColor,
        applyInversion
    )
}
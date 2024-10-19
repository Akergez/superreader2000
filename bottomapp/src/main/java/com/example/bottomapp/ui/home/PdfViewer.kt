import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.max
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.PageCurlConfig
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.PageCurlState
import eu.wewox.pagecurl.page.rememberPageCurlState
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun PdfReaderScreen(book: PdfBookDomainModel, settingsManager: SettingsManager) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(Mode.Curl) }

    val context = LocalContext.current
    val pdfFile = book.getFile();
    val pdfRendererHelper = PdfRendererHelper(context, pdfFile)
    val totalPages = pdfRendererHelper.getPageCount()

    val onPageChange = { newPage: Int ->
        currentPage = newPage
        book.saveProgress(PdfProgress(newPage))
    }

    PdfReaderUI(
        currentPage = currentPage,
        totalPages = totalPages,
        mode = mode,
        onPageChange = onPageChange,
        onBackClick = { /* Handle back */ },
        onSearchClick = { /* Handle search */ },
        onTextSettingsClick = { /* Handle text settings */ },
        onToggleMode = { newMode -> mode = newMode },
        pdfRendererHelper = pdfRendererHelper,
        settingsManager
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PdfReaderUI(
    currentPage: Int,
    totalPages: Int,
    mode: Mode,
    onPageChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTextSettingsClick: () -> Unit,
    onToggleMode: (Mode) -> Unit,
    pdfRendererHelper: PdfRendererHelper,
    settingsManager: SettingsManager
) {
    // Pass the current page to PdfContent, and update the page if needed
    PdfContent(
        currentPage = currentPage,
        totalPages = totalPages,
        mode = mode,
        pdfRendererHelper = pdfRendererHelper,
        onPageChange = onPageChange,  // Synchronize the page change
        onToggleMode = onToggleMode,
        settingsManager
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
    val context = LocalContext.current
    var applyInversion by remember { mutableStateOf(settingsManager.getDocumentsTheme("AsIs")) } // State to control inversion
    var zoomMode by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }
    if (mode == Mode.Carousel) {
        // Show Carousel mode
        PdfCarousel(
            totalPages = totalPages,
            initialPage = currentPage,  // Pass current page
            onPageChange = onPageChange,  // Update page on scroll
            onToggleMode = onToggleMode,
            pdfRendererHelper = pdfRendererHelper,
            applyInversion = applyInversion == "Invert",
            onApplyInversionToggle = { b ->
                applyInversion =  if (b) "Invert" else "AsIs";
                settingsManager.setDocumentsTheme(applyInversion)
            } // Update inversion setting
        )
    } else if (mode == Mode.Curl) {
        // Show PageCurl mode
        PageCurl(
            count = totalPages,
            state = pagerState,
            config = rememberPageCurlConfig(
                backPageContentAlpha = 0f,
                backPageColor = if (applyInversion == "Invert") MaterialTheme.colorScheme.background else Color.White
            )
        ) { index ->
            PdfPage(
                page = index,
                pdfRendererHelper = pdfRendererHelper,
                onPageChange = onPageChange,
                applyInversion = applyInversion == "Invert", // Update page on swipe
                mode = Mode.Curl,
                onToggleMode = onToggleMode
            )
        }
    } else if (mode == Mode.Zoom) {
        PdfPage(
            page = pagerState.current,
            mode = mode,
            onToggleMode = onToggleMode,
            pdfRendererHelper = pdfRendererHelper,
            onPageChange = onPageChange,
            applyInversion = applyInversion == "Invert"
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
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
    val pagerState = rememberPagerState(initialPage = initialPage)

    // Emit page change when the page is scrolled
    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            count = totalPages,
            state = pagerState,
            modifier = Modifier.weight(0.9f) // Full screen carousel
        ) { page ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Card(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.Center)
                ) {
                    PdfPage(
                        page = page,
                        pdfRendererHelper = pdfRendererHelper,
                        onPageChange = {},
                        onToggleMode = onToggleMode,
                        mode = Mode.Carousel,
                        applyInversion = applyInversion
                    )
                }
            }
        }

        // Page indicator and inversion toggle for the carousel
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${pagerState.currentPage + 1} of $totalPages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = if (applyInversion) "Inversion: On" else "Inversion: Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { onApplyInversionToggle(!applyInversion) })
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
    if (mode == Mode.Zoom) {
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

            Log.e("TAG", "PdfPage: $scale")
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
    } else {
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
                            onToggleMode(if (mode == Mode.Curl) Mode.Carousel else Mode.Curl)
                        }
                    )
                }
                .background(color = MaterialTheme.colorScheme.background)
        ) {

            Log.e("TAG", "PdfPage: $scale")
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
    val bitmap = pdfRendererHelper.openPage(pageIndex)
    var backgroundColor = Color.Black
    if (!applyInversion) {
        backgroundColor = Color.White
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvas = drawContext.canvas.nativeCanvas
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val scaleFactor = scale * minOf(
            canvasWidth.toFloat() / bitmapWidth,
            canvasHeight.toFloat() / bitmapHeight
        )
        val scaledWidth = (bitmapWidth * scaleFactor).toInt()
        val scaledHeight = (bitmapHeight * scaleFactor).toInt()
        val left = (canvasWidth - scaledWidth) / 2 + offsetX.toInt()
        val top = (canvasHeight - scaledHeight) / 2 + offsetY.toInt()

        val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)

        canvas.drawColor(backgroundColor.toArgb())

        if (applyInversion) {
            val colorMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }

            val paint = android.graphics.Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            canvas.drawBitmap(bitmap, null, destRect, paint)
            paint.colorFilter = PorterDuffColorFilter(backgroundColor.toArgb(), PorterDuff.Mode.XOR)
            canvas.drawBitmap(bitmap, null, destRect, paint)
        } else {
            canvas.drawBitmap(bitmap, null, destRect, null)
        }
    }
}
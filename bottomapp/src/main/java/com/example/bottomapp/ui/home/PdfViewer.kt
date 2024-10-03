import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.max
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
fun PdfReaderScreen(pdfFile: File) {
    var currentPage by remember { mutableIntStateOf(1) }
    var showCarousel by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val pdfRendererHelper = PdfRendererHelper(context, pdfFile)
    val totalPages = pdfRendererHelper.getPageCount()

    PdfReaderUI(
        currentPage = currentPage,
        totalPages = totalPages,
        showCarousel = showCarousel,
        onPageChange = { newPage -> currentPage = newPage },
        onBackClick = { /* Handle back */ },
        onSearchClick = { /* Handle search */ },
        onTextSettingsClick = { /* Handle text settings */ },
        onToggleCarousel = { showCarousel = !showCarousel },
        pdfRendererHelper = pdfRendererHelper
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PdfReaderUI(
    currentPage: Int,
    totalPages: Int,
    showCarousel: Boolean,
    onPageChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTextSettingsClick: () -> Unit,
    onToggleCarousel: () -> Unit,
    pdfRendererHelper: PdfRendererHelper
) {
    // Pass the current page to PdfContent, and update the page if needed
    PdfContent(
        currentPage = currentPage,
        totalPages = totalPages,
        showCarousel = showCarousel,
        pdfRendererHelper = pdfRendererHelper,
        onPageChange = onPageChange,  // Synchronize the page change
        onToggleCarousel = onToggleCarousel
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

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PdfContent(
    currentPage: Int,
    totalPages: Int,
    showCarousel: Boolean,
    pdfRendererHelper: PdfRendererHelper,
    onPageChange: (Int) -> Unit,
    onToggleCarousel: () -> Unit
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)
    val context = LocalContext.current
    var applyInversion by remember { mutableStateOf(true) } // State to control inversion

    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }
    if (showCarousel) {
        // Show Carousel mode
        PdfCarousel(totalPages = totalPages,
            initialPage = currentPage,  // Pass current page
            onPageChange = onPageChange,  // Update page on scroll
            onToggleCarousel = onToggleCarousel,
            pdfRendererHelper = pdfRendererHelper,
            applyInversion = applyInversion,
            onApplyInversionToggle = { b -> applyInversion = b } // Update inversion setting
        )
    } else {
        // Show PageCurl mode
        PageCurl(
            count = totalPages,
            state = pagerState,
            config = rememberPageCurlConfig(
                backPageContentAlpha = 0f,
                backPageColor = if (applyInversion) MaterialTheme.colorScheme.background else Color.White
            )
        ) { index ->
            PdfPage(
                page = index,
                showCarousel = false,
                onToggleCarousel = onToggleCarousel,
                pdfRendererHelper = pdfRendererHelper,
                onPageChange = onPageChange,
                applyInversion = applyInversion// Update page on swipe
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PdfCarousel(
    totalPages: Int,
    initialPage: Int = 0,
    onPageChange: (Int) -> Unit,
    onToggleCarousel: () -> Unit,
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
                        onToggleCarousel = onToggleCarousel,
                        showCarousel = false,
                        applyInversion = applyInversion // Pass the inversion setting
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
    showCarousel: Boolean,
    pdfRendererHelper: PdfRendererHelper,
    modifier: Modifier = Modifier,
    onToggleCarousel: () -> Unit,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean // Add this parameter
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(modifier = Modifier
        .padding(8.dp)
        .fillMaxSize()
        .clickable { onToggleCarousel() }
        .background(color = MaterialTheme.colorScheme.background) // Add shadow only in carousel mode
    ) {
        PdfViewer(
            pdfRendererHelper = pdfRendererHelper,
            pageIndex = page,
            applyInversion = applyInversion // Pass the inversion setting
        )
    }
}

@Composable
fun PdfViewer(
    pdfRendererHelper: PdfRendererHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean // Add this parameter
) {
    val bitmap = pdfRendererHelper.openPage(pageIndex)
    val isDarkTheme = isSystemInDarkTheme()
    var backgroundColor = MaterialTheme.colorScheme.background;
    if (!applyInversion) {
        backgroundColor = Color.White;
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvas = drawContext.canvas.nativeCanvas
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Calculate the scaling factor to maintain aspect ratio
        val scale =
            minOf(canvasWidth.toFloat() / bitmapWidth, canvasHeight.toFloat() / bitmapHeight)
        val scaledWidth = (bitmapWidth * scale).toInt()
        val scaledHeight = (bitmapHeight * scale).toInt()
        val left = (canvasWidth - scaledWidth) / 2
        val top = (canvasHeight - scaledHeight) / 2

        val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)

        // Set the background color from the Material theme
        canvas.drawColor(backgroundColor.toArgb())

        if (isDarkTheme && applyInversion) {
            // Create a ColorMatrix to invert colors
            val colorMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        -1f,
                        0f,
                        0f,
                        0f,
                        255f,
                        0f,
                        -1f,
                        0f,
                        0f,
                        255f,
                        0f,
                        0f,
                        -1f,
                        0f,
                        255f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f
                    )
                )
            }

            // Apply the ColorMatrix to the canvas
            val paint = android.graphics.Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            // Draw the bitmap with the color matrix applied
            canvas.drawBitmap(bitmap, null, destRect, paint)

            // Apply a PorterDuffColorFilter to invert colors only for non-transparent pixels
            paint.colorFilter = PorterDuffColorFilter(backgroundColor.toArgb(), PorterDuff.Mode.XOR)
            canvas.drawBitmap(bitmap, null, destRect, paint)
        } else {
            // Draw the bitmap without any color filter
            canvas.drawBitmap(bitmap, null, destRect, null)
        }
    }
}
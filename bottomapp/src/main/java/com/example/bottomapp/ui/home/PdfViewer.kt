import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.IntRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.max
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.PageCurlState
import eu.wewox.pagecurl.page.rememberPageCurlState
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@Composable
fun PdfReaderScreen() {
    var currentPage by remember { mutableIntStateOf(1) }  // Store the current page
    var showCarousel by remember { mutableStateOf(false) }  // Store the current mode (carousel or curl)

    val context = LocalContext.current
    val pdfFile = getFileFromAssets(context, "gold.pdf")
    val pdfRendererHelper = PdfRendererHelper(context, pdfFile)
    val totalPages = pdfRendererHelper.getPageCount()

    PdfReaderUI(
        currentPage = currentPage,
        totalPages = totalPages,
        showCarousel = showCarousel,
        onPageChange = { newPage -> currentPage = newPage },  // Update the shared page
        onBackClick = { /* Handle back */ },
        onSearchClick = { /* Handle search */ },
        onTextSettingsClick = { /* Handle text settings */ },
        onToggleCarousel = { showCarousel = !showCarousel },  // Toggle between modes
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
    @IntRange(from = 0) initialPage: Int = 0,
    max: Int = 0
): PageCurlState = rememberSaveable() {
    PageCurlState(
        initialCurrent = initialPage,
        initialMax = max
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
    LaunchedEffect(pagerState.current) {
       onPageChange(pagerState.current)
    }
    if (showCarousel) {
        // Show Carousel mode
        PdfCarousel(
            totalPages = totalPages,
            initialPage = currentPage,  // Pass current page
            onPageChange = onPageChange,  // Update page on scroll
            onToggleCarousel = onToggleCarousel,
            pdfRendererHelper = pdfRendererHelper
        )
    } else {
        // Show PageCurl mode
        PageCurl(count = totalPages, state = pagerState) { index ->
            PdfPage(
                page = index,
                showCarousel = false,
                onToggleCarousel = onToggleCarousel,
                pdfRendererHelper = pdfRendererHelper,
                onPageChange = onPageChange  // Update page on swipe
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
    pdfRendererHelper: PdfRendererHelper
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
            modifier = Modifier.weight(1f) // Full screen carousel
        ) { page ->
            PdfPage(
                page = page,
                pdfRendererHelper = pdfRendererHelper,
                onPageChange = {},
                onToggleCarousel = onToggleCarousel,
                showCarousel = false
            )
        }

        // Page indicator for the carousel
        Text(
            text = "Page ${pagerState.currentPage + 1} of $totalPages",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        )
    }
}

@Composable
fun PdfPage(
    page: Int,
    showCarousel: Boolean,
    pdfRendererHelper: PdfRendererHelper,
    modifier: Modifier = Modifier,
    onToggleCarousel: () -> Unit,
    onPageChange: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxHeight()
            .width(if (showCarousel) screenWidth * 0.9f else screenWidth)
            .clickable { onToggleCarousel() }
            .then(if (showCarousel) Modifier.shadow(8.dp) else Modifier) // Add shadow only in carousel mode
    ) {
        PdfViewer(
            pdfRendererHelper = pdfRendererHelper, pageIndex = page
        )
    }
}

@Composable
fun PdfViewer(
    pdfRendererHelper: PdfRendererHelper, pageIndex: Int, modifier: Modifier = Modifier
) {
    val bitmap = pdfRendererHelper.openPage(pageIndex)
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(400.dp) // Adjust height as needed
    )
}

fun getFileFromAssets(context: Context, fileName: String): File {
    val file = File(context.cacheDir, fileName)
    if (!file.exists()) {
        val inputStream: InputStream = context.assets.open(fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
    }
    return file
}
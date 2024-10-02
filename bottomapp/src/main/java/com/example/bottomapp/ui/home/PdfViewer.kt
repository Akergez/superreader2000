import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.page.PageCurl
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@Composable
fun PdfReaderScreen() {
    var currentPage by remember { mutableStateOf(1) }
    var showCarousel by remember { mutableStateOf(false) }
    val totalPages = 646  // Replace with actual total pages from your PDF document

    PdfReaderUI(
        currentPage = currentPage,
        totalPages = totalPages,
        showCarousel = showCarousel,
        onPageChange = { newPage -> currentPage = newPage },
        onBackClick = { /* Handle back */ },
        onSearchClick = { /* Handle search */ },
        onTextSettingsClick = { /* Handle text settings */ },
        onToggleCarousel = { showCarousel = !showCarousel }
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
    onToggleCarousel: () -> Unit
) {
    // Main content
    PdfContent(currentPage, totalPages, showCarousel, onPageChange, onToggleCarousel)
}

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PdfContent(
    currentPage: Int,
    totalPages: Int,
    showCarousel: Boolean,
    onPageChange: (Int) -> Unit,
    onToggleCarousel: () -> Unit
) {
    if (showCarousel) {
        Column {
            LazyRow(
                modifier = Modifier.fillMaxSize()
            ) {
                items(totalPages) { page ->
                    PdfPage(page = page, showCarousel = showCarousel, onToggleCarousel = onToggleCarousel)
                }
            }
            Text(
                text = "Page $currentPage of $totalPages",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    } else {
        PageCurl(count = totalPages) { index ->
            PdfPage(page = currentPage, showCarousel = showCarousel, onToggleCarousel = onToggleCarousel)
        }
    }
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

@Composable
fun PdfPage(page: Int, showCarousel: Boolean, modifier: Modifier = Modifier, onToggleCarousel: () -> Unit) {
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
        val context = LocalContext.current
        val pdfFile = getFileFromAssets(context, "gold.pdf")
        val pdfRendererHelper = PdfRendererHelper(context, pdfFile)
        PdfViewer(
            pdfRendererHelper = pdfRendererHelper,
            pageIndex = page
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
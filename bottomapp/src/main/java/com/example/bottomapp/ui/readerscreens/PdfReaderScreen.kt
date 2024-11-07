import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfFlipMode
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.PdfPage
import com.example.bottomapp.ui.readerscreens.pdfflippings.Mode
import com.example.bottomapp.ui.readerscreens.pdfflippings.PdfCarousel
import com.example.bottomapp.ui.readerscreens.pdfflippings.VerticalPdfScroll
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PdfReaderScreen(book: PdfBookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(when(settingsManager.getPdfFlipMode()) {
            PdfFlipMode.Curl -> Mode.Curl
            PdfFlipMode.Scroll -> Mode.Scroll
        })
    }

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
    val flipMode by remember { mutableStateOf(settingsManager.getPdfFlipMode()) }

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
                pdfFlipMode = flipMode,
                onPageChange = onPageChange,
                onToggleMode = onToggleMode,
                pdfRendererHelper = pdfRendererHelper,
                applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                onApplyInversionToggle = { applyInversion ->
                    settingsManager.setDocumentsTheme(if (applyInversion) DocThemeMode.Invert else DocThemeMode.AsIs)
                }
            )
        }

        Mode.Scroll -> {
            VerticalPdfScroll(
                totalPages = totalPages,
                initialPage = currentPage,
                pdfFlipMode = flipMode,
                onPageChange = onPageChange,
                onToggleMode = onToggleMode,
                pdfRendererHelper = pdfRendererHelper,
                applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
//                onApplyInversionToggle = { applyInversion ->
//                    settingsManager.setDocumentsTheme(if (applyInversion) DocThemeMode.Invert else DocThemeMode.AsIs)
//                }
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
                    pdfFlipMode = flipMode,
                    onToggleMode = onToggleMode,
                    applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                    mode = Mode.Curl
                )
            }
        }

        Mode.Zoom -> {
            PdfPage(
                page = pagerState.current,
                pdfRendererHelper = pdfRendererHelper,
                pdfFlipMode = flipMode,
                onToggleMode = onToggleMode,
                applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert,
                mode = mode
            )
        }
    }
}

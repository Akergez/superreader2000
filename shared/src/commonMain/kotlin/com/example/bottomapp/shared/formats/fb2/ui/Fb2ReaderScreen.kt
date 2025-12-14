import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.formats.fb2.Fb2BookDomainModel
import com.example.bottomapp.shared.formats.fb2.Fb2Progress
import com.example.bottomapp.shared.formats.fb2.Fb2RenderHelper
import com.example.bottomapp.shared.formats.fb2.ui.Fb2ViewerProvider
import com.example.bottomapp.shared.ui.screens.readerscreens.common.BookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.ReaderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject


@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: ISettingsManager = koinInject()) {
    var currentPage by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }
    val textMeasurer = rememberTextMeasurer()

    var componentHeight by remember { mutableIntStateOf(0) }
    var componentWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val fb2RenderHelper = remember { mutableStateOf<Fb2RenderHelper?>(null) }
    val lock = Any()

    LaunchedEffect(componentHeight) {
        CoroutineScope(Dispatchers.IO).launch {
            if (componentHeight > 1) {
                val fb2File = book.getFile()
                var newPage: Int
                synchronized(lock) {
                    if (fb2RenderHelper.value == null) {
                        fb2RenderHelper.value = Fb2RenderHelper(
                            componentWidth - (density.density * (16 + 32)).toInt(),
                            componentHeight - (density.density * (16 + 40)).toInt(),
                            textMeasurer,
                            fb2File
                        )
                    }
                    val offset = book.readProgress().characterPosition
                    newPage = if (offset > 0) {
                        fb2RenderHelper.value!!.getPageIndexByWordOffset(offset)
                    } else {
                        0
                    }

                }
                withContext(Dispatchers.Main) {
                    currentPage = newPage
                }
            }
        }
    }

    val onPageChange = { pageIndex: Int ->
        if (fb2RenderHelper.value != null) {
            val helper = fb2RenderHelper.value!!
            if (helper.pageWordSum.size > pageIndex) {
                CoroutineScope(Dispatchers.IO).launch {
                    book.saveProgress(
                        Fb2Progress(
                            helper.pageWordSum[pageIndex],
                            helper.totalWords.value ?: 0
                        )
                    )
                }
            }
            currentPage = pageIndex
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned {
            componentHeight = it.size.height
            componentWidth = it.size.width
        }) {
        if (fb2RenderHelper.value != null && currentPage != null) {
            val curlMode = remember { settingsManager.getFlipMode() }
            //Log.d("Fb2ReaderScreen", "$currentPage")
            BookContent(
                currentPage = currentPage!!,
                totalPages = fb2RenderHelper.value!!.pagesCount,
                mode = mode,
                onPageChange = onPageChange,
                onToggleMode = { newMode -> mode = newMode },
                flipMode = curlMode,
                getPage = { page -> fb2RenderHelper.value!!.openPage(page, false) },
                documentTheme = settingsManager.getDocumentsTheme(DocThemeMode.AsIs),
                updateDocumentThemeMode = { docThemeMode ->
                    settingsManager.setDocumentsTheme(
                        docThemeMode
                    )
                },
                updateFlipMode = { flipMode -> settingsManager.setFlipMode(flipMode) },
                onCarouselBack = { mode = ReaderMode.Reading },
                title = book.getMetadata().title,
                tableOfContent = fb2RenderHelper.value!!.tableOfContents,
                viewerProvider = Fb2ViewerProvider()
            )
        } else {
            //Text(getString(LocalContext.current, R.string.loading))
        }
    }

}

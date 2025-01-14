import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.renderHelper.Fb2RenderHelper
import com.example.bottomapp.ui.readerscreens.common.BookContent
import com.example.bottomapp.ui.readerscreens.common.ReaderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }
    val textMeasurer = rememberTextMeasurer()
    var isStartPageLoaded by remember {  mutableStateOf(false) }

    var componentHeight by remember { mutableIntStateOf(0) }
    var componentWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val fb2RenderHelper = remember { mutableStateOf<Fb2RenderHelper?>(null) }

    LaunchedEffect(componentHeight) {
        CoroutineScope(Dispatchers.IO).launch {
            val fb2File = book.getFile()
            if (componentHeight != 0) {
                fb2RenderHelper.value = Fb2RenderHelper(
                    componentWidth - (density.density * 16).toInt(),
                    componentHeight - (density.density * 16).toInt(),
                    textMeasurer,
                    fb2File
                )
            }
        }
    }

    LaunchedEffect(fb2RenderHelper.value) {
        if (fb2RenderHelper.value != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val offset = book.readProgress().wordOffset
                if (offset > 0) {
                    var index = 0
                    var startLoading = false
                    while (true) {
                        val wordsCount = fb2RenderHelper.value!!.pageWordSum

                        // если страница прогрузилась
                        if (wordsCount.size > index) {
                            startLoading = false
                            if (wordsCount[index] > offset) {
                                currentPage = index
                                break
                            }
                        } else {
                            if (!startLoading) {
                                fb2RenderHelper.value!!.openPage(index + 1)
                                startLoading = true
                            }
                            delay(50)
                            continue
                        }
                        index += 1
                    }
                }
                isStartPageLoaded = true
            }
        }
    }

    val onPageChange = { pageIndex: Int ->
        if (false) {
            val wordsCount = fb2RenderHelper.value!!.pageWordSum
            if (wordsCount.size > pageIndex) {
                CoroutineScope(Dispatchers.IO).launch {
                    book.saveProgress(Fb2Progress(wordsCount[pageIndex]))
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
        if (fb2RenderHelper.value != null && isStartPageLoaded) {
            val curlMode = remember { settingsManager.getFlipMode() }
            Log.d("Fb2ReaderScreen", "$currentPage")
            BookContent(
                currentPage = currentPage,
                totalPages = fb2RenderHelper.value!!.pagesCount,
                mode = mode,
                onPageChange = onPageChange,
                onToggleMode = { newMode -> mode = newMode },
                flipMode = curlMode,
                getPage = { page -> fb2RenderHelper.value!!.openPage(page) },
                documentTheme = settingsManager.getDocumentsTheme(DocThemeMode.AsIs),
                updateDocumentThemeMode = { docThemeMode ->
                    settingsManager.setDocumentsTheme(
                        docThemeMode
                    )
                },
                updateFlipMode = { flipMode -> settingsManager.setPdfFlipMode(flipMode)},
                onCarouselBack = { mode = ReaderMode.Reading },
                title = book.title,
                tableOfContent = MutableStateFlow(emptyList())
            )
        } else {
            Text(getString(LocalContext.current, R.string.loading))
        }
    }
}

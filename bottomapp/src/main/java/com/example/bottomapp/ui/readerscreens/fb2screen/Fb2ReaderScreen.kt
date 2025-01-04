import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }
    var sums = listOf<Int>()
    val textMeasurer = rememberTextMeasurer()

    var componentHeight by remember { mutableIntStateOf(0) }
    var componentWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val fb2RenderHelper = remember { mutableStateOf<Fb2RenderHelper?>(null) }

    LaunchedEffect(componentHeight) {
        Log.d("fb2ReaderScreen", "$componentHeight")
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

    val onPageChange = { newPage: Int ->
        if (sums.isNotEmpty() && newPage > 0) {
            currentPage = newPage
            CoroutineScope(Dispatchers.IO).launch {
                book.saveProgress(Fb2Progress(sums[newPage]))
            }
        }
        if (newPage == 0) {
            currentPage = 0
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned {
            componentHeight = it.size.height
            componentWidth = it.size.width
        }) {
        if (fb2RenderHelper.value != null) {
            val totalPages = fb2RenderHelper.value!!.pagesCount.collectAsState()
            val curlMode = remember { settingsManager.getFlipMode() }

            sums =
                remember { calculatePrefixSums(fb2RenderHelper.value!!.pageCache.values.map { it.value.text }) }
            val offset = book.readProgress().wordOffset

            if (offset > 0) {
                for (i in sums.indices) {
                    if (sums[i] > offset) {
                        currentPage = i - 1
                        break
                    }
                }
            }

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
                    settingsManager.setDocumentsTheme(docThemeMode)
                }
            )
        } else {
            Text(getString(LocalContext.current, R.string.loading))
        }
    }

}

fun calculatePrefixSums(pages: List<String?>): List<Int> {
    val prefixSums = mutableListOf<Int>()
    var currentSum = 0

    for (page in pages) {
        prefixSums.add(currentSum)
        if (page == null) {
            break
        }
        currentSum += page.length
    }

    return prefixSums
}


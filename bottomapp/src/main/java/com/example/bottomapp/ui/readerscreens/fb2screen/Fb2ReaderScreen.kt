//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.RenderHelper.Fb2RenderHelper
import com.example.bottomapp.backend.RenderHelper.IRenderHelper
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.BookContent
import com.example.bottomapp.ui.readerscreens.common.ReaderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }
    val context = LocalContext.current
    val fb2File = remember { book.getFile() }
    var sums by remember { mutableStateOf(listOf<Int>()) }
    val textMeasurer = rememberTextMeasurer()

    var componentHeight by remember { mutableStateOf(0.dp) }
    var componentWidth by remember { mutableStateOf(0.dp) }
    // get local density from composable
    val density = LocalDensity.current


    val displayMetrics = context.resources.displayMetrics
    val navBarHeightPx = LocalDensity.current.run { 400.dp.toPx() }
    val maxWidth = displayMetrics.widthPixels - (displayMetrics.density * 16).toInt()
    val maxHeight = displayMetrics.heightPixels - navBarHeightPx.toInt()

    val aa = LocalDensity.current.run { componentWidth.toPx() }
    val bb = LocalDensity.current.run { componentHeight.toPx() }

    val fb2RenderHelper = remember { mutableStateOf<Fb2RenderHelper?>(null) }
    val isLoaded = remember { mutableStateOf(false) }


    LaunchedEffect(componentHeight) {
        launch(Dispatchers.IO) {
            if(componentHeight.value != 0f) {
                fb2RenderHelper.value = Fb2RenderHelper(aa.toInt(), bb.toInt(), textMeasurer, fb2File)
                fb2RenderHelper.value!!.Init(16)
                isLoaded.value = true;
            }
        }
    }

    val onPageChange = { newPage: Int ->
        if (sums.isNotEmpty() && newPage > 0) {
            currentPage = newPage
            book.saveProgress(Fb2Progress(sums[newPage]))
        }
        if (newPage == 0) {
            currentPage = 0
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned {
            componentHeight = with(density) {
                it.size.height.toDp()
            }

            componentWidth = with(density) {
                it.size.width.toDp()
            }


        }) {

        if (isLoaded.value) {
            val totalPages = remember { fb2RenderHelper.value!!.getPageCount() }
            val curlMode = remember { settingsManager.getCurlMode() }

            sums = remember { calculatePrefixSums(fb2RenderHelper.value!!.pages) }
            val offset = book.readProgress().wordOffset

            if (offset > 0) {
                for (i in 0..sums.size) {
                    if (sums[i] > offset) {
                        currentPage = i - 1
                        break
                    }
                }
            }

            BookContent(
                currentPage = currentPage,
                totalPages = totalPages,
                mode = mode,
                renderHelper = fb2RenderHelper.value!!,
                onPageChange = onPageChange,
                onToggleMode = { newMode -> mode = newMode },
                settingsManager = settingsManager,
                curlMode = curlMode,
            )

        } else {
            Text("Loading")
        }
    }

}

fun calculatePrefixSums(pages: List<String>): List<Int> {
    val prefixSums = mutableListOf<Int>()
    var currentSum = 0

    for (page in pages) {
        prefixSums.add(currentSum)
        currentSum += page.length
    }

    return prefixSums
}


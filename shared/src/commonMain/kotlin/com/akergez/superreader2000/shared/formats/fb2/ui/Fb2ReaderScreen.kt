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
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.formats.fb2.Fb2BookDomainModel
import com.akergez.superreader2000.shared.formats.fb2.Fb2Progress
import com.akergez.superreader2000.shared.formats.fb2.Fb2RenderHelper
import com.akergez.superreader2000.shared.formats.fb2.RenderConfig
import com.akergez.superreader2000.shared.formats.fb2.ui.Fb2ViewerProvider
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.BookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.ReaderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun Fb2ReaderScreen(
    book: Fb2BookDomainModel,
    settingsManager: ISettingsManager = koinInject(),
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var currentPage by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }

    var componentWidth by remember { mutableIntStateOf(0) }
    var componentHeight by remember { mutableIntStateOf(0) }

    val renderConfig = remember {
        MutableStateFlow(RenderConfig(pageWidth = 0, pageHeight = 0))
    }

    val fb2RenderHelper = remember { mutableStateOf<Fb2RenderHelper?>(null) }

    LaunchedEffect(componentWidth, componentHeight) {
        println(">>> LaunchedEffect w=$componentWidth h=$componentHeight")
        if (componentWidth <= 0 || componentHeight <= 0) return@LaunchedEffect

        val paddingPx = (density.density * (16 + 32)).toInt()
        val topBarPx  = (density.density * (16 + 40)).toInt()

        val newConfig = RenderConfig(
            pageWidth  = (componentWidth - paddingPx).coerceAtLeast(1),
            pageHeight = (componentHeight - topBarPx).coerceAtLeast(1),
        )
        println(">>> newConfig=$newConfig")

        if (fb2RenderHelper.value == null) {
            println(">>> creating helper")
            renderConfig.value = newConfig

            val helper = withContext(Dispatchers.IO) {
                Fb2RenderHelper(renderConfig, textMeasurer, book.getFile())
            }
            fb2RenderHelper.value = helper
            println(">>> helper created")

            val offset = book.readProgress().characterPosition
            println(">>> readProgress offset=$offset")
            currentPage = if (offset > 0) {
                withContext(Dispatchers.IO) { helper.getPageIndexByWordOffset(offset) }
            } else {
                0
            }
            println(">>> currentPage=$currentPage")
        } else {
            println(">>> updating config only")
            renderConfig.value = newConfig
        }
    }

    val onPageChange: (Int) -> Unit = { pageIndex: Int ->
        val helper = fb2RenderHelper.value
        if (helper != null) {
            currentPage = pageIndex
            if (helper.pageWordSum.size > pageIndex) {
                CoroutineScope(Dispatchers.IO).launch {
                    book.saveProgress(
                        Fb2Progress(
                            characterPosition = helper.pageWordSum[pageIndex],
                            totalCharacters   = helper.totalWords.value ?: 0,
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                componentWidth  = it.size.width
                componentHeight = it.size.height
                println(">>> onGloballyPositioned w=${it.size.width} h=${it.size.height}")
            }
    ) {
        val helper = fb2RenderHelper.value
        val page   = currentPage
        println(">>> render: helper=${helper != null} page=$page")

        if (helper != null && page != null) {
            val curlMode = remember { settingsManager.getFlipMode() }
            BookContent(
                currentPage             = page,
                totalPages              = helper.pagesCount,
                mode                    = mode,
                onPageChange            = onPageChange,
                onToggleMode            = { newMode -> mode = newMode },
                flipMode                = curlMode,
                getPage                 = { p -> helper.openPage(p, false) },
                documentTheme           = settingsManager.getDocumentsTheme(DocThemeMode.AsIs),
                updateDocumentThemeMode = { settingsManager.setDocumentsTheme(it) },
                updateFlipMode          = { settingsManager.setFlipMode(it) },
                onCarouselBack          = { mode = ReaderMode.Reading },
                title                   = book.getMetadata().title,
                tableOfContent          = helper.tableOfContents,
                viewerProvider          = Fb2ViewerProvider(),
            )
        }
    }
}
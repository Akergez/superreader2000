import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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

    DisposableEffect(Unit) {
        onDispose {
            fb2RenderHelper.value?.close()
        }
    }

    // LaunchedEffect 1: создаём/обновляем конфиг при изменении размера.
    // Намеренно НЕ устанавливает currentPage — он устанавливается отдельным эффектом ниже,
    // чтобы изменения размера не отменяли ожидание пагинации.
    LaunchedEffect(componentWidth, componentHeight) {
        println(">>> LaunchedEffect[size] w=$componentWidth h=$componentHeight helper=${fb2RenderHelper.value != null}")
        if (componentWidth <= 0 || componentHeight <= 0) return@LaunchedEffect

        val topBarPx  = (density.density * (16 + 40)).toInt()
        // Горизонтальный отступ: 16dp в пикселях, симметрично с обеих сторон.
        val paddingH  = density.density * 32f

        val isLandscape = componentWidth.toFloat() / componentHeight > 4f / 3f
        val pagesPerSpread = if (isLandscape) 2 else 1
        val pageCanvasWidth = componentWidth / pagesPerSpread

        val newConfig = RenderConfig(
            pageWidth    = (pageCanvasWidth - (paddingH * 2).toInt()).coerceAtLeast(1),
            pageHeight   = (componentHeight - topBarPx).coerceAtLeast(1),
            canvasWidth  = pageCanvasWidth,
            canvasHeight = componentHeight,
            paddingH     = paddingH,
        )
        println(">>> newConfig=$newConfig")

        if (fb2RenderHelper.value == null) {
            println(">>> creating helper")
            renderConfig.value = newConfig
            val helper = withContext(Dispatchers.IO) {
                Fb2RenderHelper(renderConfig, textMeasurer, book.getFile())
            }
            fb2RenderHelper.value = helper
            println(">>> helper created, stored")
        } else {
            println(">>> updating config only")
            renderConfig.value = newConfig
        }
    }

    // LaunchedEffect 2: устанавливаем начальную страницу как только хелпер готов.
    // Ключ — сам объект хелпера, поэтому повторные изменения размера не отменяют это ожидание.
    LaunchedEffect(fb2RenderHelper.value) {
        val helper = fb2RenderHelper.value ?: return@LaunchedEffect
        println(">>> LaunchedEffect[helper] restoring progress")
        val offset = book.readProgress().characterPosition
        println(">>> readProgress offset=$offset")
        currentPage = if (offset > 0) {
            withContext(Dispatchers.IO) {
                helper.totalWords.filterNotNull().first()
                helper.getPageIndexByWordOffset(offset)
            }
        } else {
            0
        }
        println(">>> currentPage=$currentPage")
    }

    val onChapterNavigate: (Int) -> Unit = { wordOffset: Int ->
        CoroutineScope(Dispatchers.IO).launch {
            val helper = fb2RenderHelper.value ?: return@launch
            helper.totalWords.filterNotNull().first()
            val page = helper.getPageIndexByWordOffset(wordOffset)
            withContext(Dispatchers.Main) {
                currentPage = page
                mode = ReaderMode.Reading
            }
            if (helper.pageWordSum.size > page) {
                book.saveProgress(
                    Fb2Progress(
                        characterPosition = helper.pageWordSum[page],
                        totalCharacters   = helper.totalWords.value ?: 0,
                    )
                )
            }
        }
    }

    val onPageChange: (Int) -> Unit = { pageIndex: Int ->
        val helper = fb2RenderHelper.value
        if (helper != null) {
            val totalPgs = helper.pagesCount.value ?: Int.MAX_VALUE
            if (pageIndex >= totalPgs) {
                // Значение пришло из ещё не отпагинированной записи TOC — это word-offset,
                // а не номер страницы. Конвертируем асинхронно.
                CoroutineScope(Dispatchers.IO).launch {
                    val page = helper.getPageIndexByWordOffset(pageIndex)
                    withContext(Dispatchers.Main) { currentPage = page }
                    if (helper.pageWordSum.size > page) {
                        book.saveProgress(
                            Fb2Progress(
                                characterPosition = helper.pageWordSum[page],
                                totalCharacters   = helper.totalWords.value ?: 0,
                            )
                        )
                    }
                }
            } else {
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val newW = it.size.width
                val newH = it.size.height
                if (newW != componentWidth || newH != componentHeight) {
                    println(">>> SIZE CHANGED: ${componentWidth}x${componentHeight} -> ${newW}x${newH}")
                }
                componentWidth  = newW
                componentHeight = newH
            }
    ) {
        val helper = fb2RenderHelper.value
        val page   = currentPage
        println(">>> render: helper=${helper != null} page=$page")

        if (helper != null && page != null) {
            var curlMode by remember { mutableStateOf(settingsManager.getFlipMode()) }
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
                updateFlipMode          = { curlMode = it; settingsManager.setFlipMode(it) },
                onCarouselBack          = { mode = ReaderMode.Reading },
                title                   = book.getMetadata().title,
                tableOfContent          = helper.tableOfContents,
                viewerProvider          = Fb2ViewerProvider(),
                onChapterNavigate       = onChapterNavigate,
            )
        }
    }
}
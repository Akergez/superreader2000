import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ALL_DOC_THEME_MODES
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.formats.fb2.Fb2BookDomainModel
import com.akergez.superreader2000.shared.formats.fb2.Fb2Progress
import com.akergez.superreader2000.shared.formats.fb2.Fb2RenderHelper
import com.akergez.superreader2000.shared.formats.fb2.RenderConfig
import com.akergez.superreader2000.shared.formats.fb2.ui.Fb2ViewerProvider
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.BookContent
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.ReaderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.Font
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.literata_light
import superreader2000.shared.generated.resources.roboto_variable

@Composable
fun Fb2ReaderScreen(
    book: Fb2BookDomainModel,
    settingsManager: ISettingsManager = koinInject(),
) {
    val textMeasurer = rememberTextMeasurer()
    val literataFamily = FontFamily(Font(Res.font.literata_light))
    val robotoFamily = FontFamily(Font(Res.font.roboto_variable))
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val displayCutout = WindowInsets.displayCutout
    val cutoutLeft = displayCutout.getLeft(density, layoutDirection)
    val cutoutTop = displayCutout.getTop(density)
    val cutoutRight = displayCutout.getRight(density, layoutDirection)
    val cutoutBottom = displayCutout.getBottom(density)
    val progressScope = remember(book) {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
    var progressSaveJob by remember(book) { mutableStateOf<Job?>(null) }

    var currentPage by remember(book) { mutableStateOf<Int?>(null) }
    // Устойчивая позиция в книге. Номер страницы меняется вместе с геометрией окна.
    var currentWordOffset by remember(book) {
        mutableIntStateOf(book.readProgress().characterPosition.coerceAtLeast(0))
    }
    var isRestoringLayout by remember(book) { mutableStateOf(true) }
    var isPageControlSynchronized by remember(book) { mutableStateOf(false) }
    var mode by remember(book) { mutableStateOf(ReaderMode.Reading) }
    var readerFont by remember(book) { mutableStateOf(settingsManager.getReaderFont()) }
    var readerFontSize by remember(book) { mutableStateOf(settingsManager.getReaderFontSize()) }
    var documentTheme by remember(book) {
        mutableStateOf(settingsManager.getTextDocumentsTheme(DocThemeMode.AsIs))
    }

    var componentWidth by remember { mutableIntStateOf(0) }
    var componentHeight by remember { mutableIntStateOf(0) }

    val renderConfig = remember(book) {
        MutableStateFlow(RenderConfig(pageWidth = 0, pageHeight = 0))
    }

    val fb2RenderHelper = remember(book) { mutableStateOf<Fb2RenderHelper?>(null) }

    // Helper создаётся независимо от измерений окна. Иначе перезапуск size-effect
    // может оставить уже запущенный парсер без владельца и создать второй helper.
    LaunchedEffect(book) {
        val file = withContext(Dispatchers.IO) { book.getFile() }
        fb2RenderHelper.value = Fb2RenderHelper(renderConfig, textMeasurer, file)
    }

    val latestPage = rememberUpdatedState(currentPage)
    val latestWordOffset = rememberUpdatedState(currentWordOffset)
    val latestIsRestoring = rememberUpdatedState(isRestoringLayout)
    val latestHelper = rememberUpdatedState(fb2RenderHelper.value)

    DisposableEffect(book) {
        onDispose {
            progressSaveJob?.cancel()
            val helper = latestHelper.value
            val page = latestPage.value
            val fallbackOffset = latestWordOffset.value
            val restoring = latestIsRestoring.value
            progressScope.launch {
                try {
                    val finalOffset = if (!restoring && helper != null && page != null) {
                        helper.getPageStartWordOffsetAfterPagination(page) ?: fallbackOffset
                    } else {
                        fallbackOffset
                    }
                    book.saveProgress(
                        Fb2Progress(
                            characterPosition = finalOffset.coerceAtLeast(0),
                            totalCharacters = helper?.totalWords?.value
                                ?: book.readProgress().totalCharacters,
                        )
                    )
                    book.flushProgress()
                } finally {
                    helper?.close()
                    progressScope.cancel()
                }
            }
        }
    }

    // LaunchedEffect 1: создаём/обновляем конфиг при изменении размера.
    LaunchedEffect(
        componentWidth,
        componentHeight,
        cutoutLeft,
        cutoutTop,
        cutoutRight,
        cutoutBottom,
        readerFont,
        readerFontSize,
    ) {
        if (componentWidth <= 0 || componentHeight <= 0) return@LaunchedEffect

        val availableWidth = (componentWidth - cutoutLeft - cutoutRight).coerceAtLeast(1)
        val availableHeight = (componentHeight - cutoutTop - cutoutBottom).coerceAtLeast(1)
        val shortestSideDp = minOf(availableWidth, availableHeight) / density.density
        val isTablet = shortestSideDp >= 600f
        val paddingH = density.density * if (isTablet) 44f else 24f
        val paddingV = density.density * if (isTablet) 36f else 20f
        val isLandscape = availableWidth.toFloat() / availableHeight > 4f / 3f
        val pagesPerSpread = if (isLandscape) 2 else 1
        val pageCanvasWidth = availableWidth / pagesPerSpread

        val newConfig = RenderConfig(
            pageWidth    = (pageCanvasWidth - (paddingH * 2).toInt()).coerceAtLeast(1),
            pageHeight   = (availableHeight - (paddingV * 2).toInt()).coerceAtLeast(1),
            canvasWidth  = pageCanvasWidth,
            canvasHeight = availableHeight,
            paddingH     = paddingH,
            paddingV     = paddingV,
            density      = density.density,
            fontSize     = readerFontSize.sp,
            fontFamily   = when (readerFont) {
                ReaderFont.Literata -> literataFamily
                ReaderFont.Roboto -> robotoFamily
            },
        )

        val helper = fb2RenderHelper.value
        if (helper == null) {
            renderConfig.value = newConfig
        } else if (renderConfig.value != newConfig) {
            val savedOffset = currentWordOffset

            isRestoringLayout = true
            isPageControlSynchronized = false
            try {
                renderConfig.value = newConfig
                // Немедленно очищаем границы старой геометрии. Иначе точный поиск может
                // успеть вернуть старый номер страницы до debounce перепагинации.
                withContext(Dispatchers.IO) { helper.repaginate() }

                currentPage = null
                val exact = withContext(Dispatchers.IO) {
                    helper.getPageIndexByWordOffset(savedOffset)
                }
                currentWordOffset = savedOffset
                currentPage = exact
            } finally {
                isRestoringLayout = false
            }
        }
    }

    // LaunchedEffect 2: устанавливаем начальную страницу как только хелпер готов.
    // Ключ — сам объект хелпера, поэтому повторные изменения размера не отменяют это ожидание.
    LaunchedEffect(fb2RenderHelper.value) {
        val helper = fb2RenderHelper.value ?: return@LaunchedEffect
        val offset = book.readProgress().characterPosition
        currentWordOffset = offset.coerceAtLeast(0)
        isRestoringLayout = true
        isPageControlSynchronized = false
        try {
            if (offset <= 0) {
                currentPage = 0
            } else {
                withContext(Dispatchers.IO) { helper.totalWords.filterNotNull().first() }
                currentPage = null
                val exact = withContext(Dispatchers.IO) {
                    helper.getPageIndexByWordOffset(offset)
                }
                currentWordOffset = offset
                currentPage = exact
            }
        } finally {
            isRestoringLayout = false
        }
    }

    val onChapterNavigate: (Int) -> Unit = { wordOffset: Int ->
        CoroutineScope(Dispatchers.IO).launch {
            val helper = fb2RenderHelper.value ?: return@launch
            helper.totalWords.filterNotNull().first()
            val page = helper.getPageIndexByWordOffset(wordOffset)
            withContext(Dispatchers.Main) {
                isPageControlSynchronized = false
                currentWordOffset = wordOffset
                currentPage = page
                mode = ReaderMode.Reading
            }
            book.saveProgress(
                Fb2Progress(
                    characterPosition = wordOffset,
                    totalCharacters   = helper.totalWords.value ?: 0,
                )
            )
        }
    }

    val onPageChange: (Int) -> Unit = onPageChange@ { requestedPage: Int ->
        // Pager может прислать временный номер, когда pagesCount меняется во время reflow.
        // Он не должен затирать устойчивый word-offset пользователя.
        if (isRestoringLayout) return@onPageChange

        val helper = fb2RenderHelper.value
        if (helper != null) {
            val pageIndex = requestedPage.coerceAtLeast(0)

            // Листалки публикуют текущую страницу сразу после создания и во время
            // программного восстановления. Это не действие читателя и не новый progress.
            if (!isPageControlSynchronized) {
                if (pageIndex == currentPage) {
                    isPageControlSynchronized = true
                }
                return@onPageChange
            }

            currentPage = pageIndex

            // Для уже показанной страницы граница обычно известна — сохраняем её
            // немедленно, не ожидая очередной фоновой пагинации.
            val knownOffset = helper.getPageStartWordOffset(pageIndex)
            if (knownOffset != null) {
                currentWordOffset = knownOffset
                progressSaveJob?.cancel()
                progressSaveJob = progressScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    book.saveProgress(
                        Fb2Progress(
                            characterPosition = knownOffset,
                            totalCharacters = helper.totalWords.value
                                ?: book.readProgress().totalCharacters,
                        )
                    )
                }
            } else {
                progressSaveJob?.cancel()
                progressSaveJob = progressScope.launch {
                    val wordOffset = helper.getPageStartWordOffsetAfterPagination(pageIndex)
                        ?: return@launch
                    val stillCurrent = withContext(Dispatchers.Main) {
                        if (currentPage == pageIndex) {
                            currentWordOffset = wordOffset
                            true
                        } else {
                            false
                        }
                    }
                    if (!stillCurrent) return@launch
                    book.saveProgress(
                        Fb2Progress(
                            characterPosition = wordOffset,
                            totalCharacters = helper.totalWords.value
                                ?: book.readProgress().totalCharacters,
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
            val pageProvider: (Int) -> StateFlow<Fb2BookContent> =
                remember(helper) { { pageIndex -> helper.openPage(pageIndex, false) } }
            BookContent(
                currentPage             = page,
                totalPages              = helper.pagesCount,
                mode                    = mode,
                onPageChange            = onPageChange,
                onToggleMode            = { newMode -> mode = newMode },
                flipMode                = curlMode,
                getPage                 = pageProvider,
                documentTheme           = documentTheme,
                availableDocumentThemes = ALL_DOC_THEME_MODES,
                updateDocumentThemeMode = {
                    documentTheme = it
                    settingsManager.setTextDocumentsTheme(it)
                },
                updateFlipMode          = { curlMode = it; settingsManager.setFlipMode(it) },
                readerFont              = readerFont,
                readerFontSize          = readerFontSize,
                readerTypographyEnabled = true,
                updateReaderFont        = {
                    readerFont = it
                    settingsManager.setReaderFont(it)
                },
                updateReaderFontSize    = {
                    readerFontSize = it
                    settingsManager.setReaderFontSize(it)
                },
                onCarouselBack          = { mode = ReaderMode.Reading },
                title                   = book.getMetadata().title,
                tableOfContent          = helper.tableOfContents,
                viewerProvider          = Fb2ViewerProvider(documentTheme),
                onChapterNavigate       = onChapterNavigate,
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

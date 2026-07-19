package com.akergez.superreader2000.shared.ui.screens.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.BINARY_DOC_THEME_MODES
import com.akergez.superreader2000.shared.backend.settings.DEFAULT_READER_FONT_SIZE
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.formats.pdf.TOCEntry
import com.akergez.superreader2000.shared.ui.IBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings.CasualCurl
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings.PageCarousel
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings.PageCurl
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings.VerticalBookScroll
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.components.SettingsModal
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ZoomState

@Composable
fun <TBookContent>BookContent(
    currentPage: Int,
    title: String,
    totalPages: StateFlow<Int?>,
    tableOfContent: StateFlow<List<TOCEntry>>,
    mode: ReaderMode,
    flipMode: FlipMode,
    onPageChange: (Int) -> Unit,
    onToggleMode: (ReaderMode) -> Unit,
    onCarouselBack: () -> Unit,
    modifier: Modifier = Modifier,
    getPage: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    documentTheme: DocThemeMode,
    availableDocumentThemes: List<DocThemeMode> = BINARY_DOC_THEME_MODES,
    updateDocumentThemeMode: (DocThemeMode) -> Unit,
    updateFlipMode: (FlipMode) -> Unit,
    readerFont: ReaderFont = ReaderFont.Literata,
    readerFontSize: Float = DEFAULT_READER_FONT_SIZE,
    readerTypographyEnabled: Boolean = false,
    updateReaderFont: (ReaderFont) -> Unit = {},
    updateReaderFontSize: (Float) -> Unit = {},
    onChapterNavigate: (Int) -> Unit = { page -> onPageChange(page); onToggleMode(ReaderMode.Reading) },
) where TBookContent : IBookContent {
    //Log.d("BookContent", "mode = $mode, flipMode = $flipMode, currentPage = $currentPage")

    val haptic = LocalHapticFeedback.current
    val readerBackground = when (documentTheme) {
        DocThemeMode.AsIs -> Color.White
        DocThemeMode.Invert -> Color.Black
        DocThemeMode.SoftLight -> Color(0xFFD6D3CC)
        DocThemeMode.SoftDark -> Color(0xFF242424)
        DocThemeMode.MaterialYou -> MaterialTheme.colorScheme.surface
    }
    val readerForeground = when (documentTheme) {
        DocThemeMode.AsIs -> Color.Black
        DocThemeMode.Invert -> Color.White
        DocThemeMode.SoftLight -> Color(0xFF353535)
        DocThemeMode.SoftDark -> Color(0xFFD0D0D0)
        DocThemeMode.MaterialYou -> MaterialTheme.colorScheme.onSurface
    }

    val showSettingsModal = remember { mutableStateOf(false) }
    // Метка времени последнего закрытия модала — для защиты от ghost-click на macOS:
    // при закрытии Popup-окна ModalBottomSheet macOS "прокидывает" click в нижнее окно,
    // и Settings-кнопка срабатывает повторно. Игнорируем клик в течение 400 мс.
    val lastModalCloseMs = remember { LongArray(1) }

    println(">>> BookContent recompose: mode=$mode flipMode=$flipMode showSettings=${showSettingsModal.value}")

    val actualMode = rememberUpdatedState(mode)
    val coroutineScope = rememberCoroutineScope()

    val onToggleModeWrapper = { newMode: ReaderMode, zoomState: ZoomState ->
        onToggleMode(newMode)
        coroutineScope.launch { zoomState.reset() }
    }

    val handleZoomState =
        { zoomState: ZoomState ->
            val newScale = zoomState.scale
            println(">>> handleZoomState: scale=$newScale mode=${actualMode.value}")
            if (newScale <= 0.9f) {
                if (actualMode.value != ReaderMode.Carousel) {
                    //Log.d("BookContent", "Updating mode from ${actualMode.value} to Carousel")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleModeWrapper(ReaderMode.Carousel, zoomState)
                }
            }

            if (newScale > 1.05f) {
                if (actualMode.value != ReaderMode.Reading) {
               //     Log.d("BookContent", "Updating mode from ${actualMode.value} to Reading")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleModeWrapper(ReaderMode.Reading, zoomState)
                }
            }
        }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Возвращаем фокус на BookContent после закрытия модала,
    // иначе пробел перестаёт работать (фокус остаётся на Popup-окне ModalBottomSheet).
    LaunchedEffect(showSettingsModal.value) {
        if (!showSettingsModal.value) {
            focusRequester.requestFocus()
        }
    }

    // Возвращаем фокус при смене режима, иначе после перехода Chapters→Reading
    // фокус остаётся на утраченном LazyColumn и пробел перестаёт работать.
    LaunchedEffect(mode) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBackground)
            .displayCutoutPadding()
            .focusRequester(focusRequester)
            .focusTarget()
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyDown) {
                    if (mode != ReaderMode.Reading) {
                        onToggleMode(ReaderMode.Reading)
                        true
                    } else {
                        onToggleMode(ReaderMode.Carousel)
                        true
                    }
                } else {
                    false
                }
            }
    )
    {
        when (mode) {
            ReaderMode.Carousel -> {
                PageCarousel(
                    totalPages = totalPages,
                    initialPage = currentPage,
                    onPageChange = onPageChange,
                    onBack = { onCarouselBack() },
                    applyInversion = documentTheme == com.akergez.superreader2000.shared.backend.settings.DocThemeMode.Invert,
                    backgroundColor = readerBackground,
                    contentColor = readerForeground,
                    modifier = modifier
                        .fillMaxSize()
                        .background(readerBackground),
                    bookContent = getPage,
                    title = title,
                    onSettingsClick = {
                        val elapsed = System.currentTimeMillis() - lastModalCloseMs[0]
                        println(">>> onSettingsClick: elapsed=${elapsed}ms since last close")
                        if (elapsed > 400) {
                            showSettingsModal.value = !showSettingsModal.value
                        }
                    },
                    onChaptersClick = { onToggleMode(ReaderMode.Chapters) },
                    zoomState = handleZoomState,
                    viewerProvider = viewerProvider
                )
                if (showSettingsModal.value) {
                    if (showSettingsModal.value) {
                        SettingsModal(
                            documentTheme = documentTheme,
                            availableDocumentThemes = availableDocumentThemes,
                            onDocumentThemeChange = updateDocumentThemeMode,
                            onVerticalScroll = {
                                println(">>> onVerticalScroll called: newMode=$it")
                                updateFlipMode(it)
                            },
                            scrollMode = flipMode,
                            readerFont = readerFont,
                            readerFontSize = readerFontSize,
                            readerTypographyEnabled = readerTypographyEnabled,
                            onReaderFontChange = updateReaderFont,
                            onReaderFontSizeChange = updateReaderFontSize,
                            onStateChange = {
                                if (!it) lastModalCloseMs[0] = System.currentTimeMillis()
                                showSettingsModal.value = it
                            }
                        )
                    }
                }
            }

            ReaderMode.Reading -> {
                when (flipMode) {
                    FlipMode.PagingCurl -> PageCurl(
                        totalPages = totalPages,
                        onPageChange = onPageChange,
                        currentPage = currentPage,
                        getPage = getPage,
                        documentTheme = documentTheme,
                        modifier = modifier,
                        zoomState = handleZoomState,
                        viewerProvider = viewerProvider
                    )

                    FlipMode.VerticalScroll -> VerticalBookScroll(
                        totalPages = totalPages,
                        initialPage = currentPage,
                        onPageChange = onPageChange,
                        applyInversion = documentTheme == DocThemeMode.Invert,
                        getPage = getPage,
                        zoomState = handleZoomState,
                        viewerProvider = viewerProvider
                    )

                    FlipMode.Casual -> CasualCurl(
                        totalPages = totalPages,
                        onPageChange = onPageChange,
                        applyInversion = documentTheme == DocThemeMode.Invert,
                        backgroundColor = readerBackground,
                        initialPage = currentPage,
                        getPage = getPage,
                        zoomState = handleZoomState,
                        viewerProvider = viewerProvider
                    )
                }
            }

            ReaderMode.Chapters -> ChapterScreen(
                onChapterClick = { onChapterNavigate(it) },
                tableOfContents = tableOfContent,
                onBack = { onToggleMode(ReaderMode.Carousel) }
            )
        }
    }
}

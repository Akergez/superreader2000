package com.example.bottomapp.shared.ui.screens.readerscreens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.renderHelper.TOCEntry
import com.example.bottomapp.shared.ui.IBookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings.CasualCurl
import com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings.PageCarousel
import com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings.PageCurl
import com.example.bottomapp.shared.ui.screens.readerscreens.common.bookFlippings.VerticalBookScroll
import com.example.bottomapp.shared.ui.screens.readerscreens.common.components.SettingsModal
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
    updateDocumentThemeMode: (DocThemeMode) -> Unit,
    updateFlipMode: (FlipMode) -> Unit
) where TBookContent : IBookContent {
    //Log.d("BookContent", "mode = $mode, flipMode = $flipMode, currentPage = $currentPage")

    val haptic = LocalHapticFeedback.current

    val showSettingsModal = remember { mutableStateOf(false) }

    val actualMode = rememberUpdatedState(mode)
    val coroutineScope = rememberCoroutineScope()

    val onToggleModeWrapper = { newMode: ReaderMode, zoomState: ZoomState ->
        onToggleMode(newMode)
        coroutineScope.launch { zoomState.reset() }
    }

    val handleZoomState =
        { zoomState: ZoomState ->
            val newScale = zoomState.scale
           // Log.d("BookContent", "New scale: $newScale")
            if (newScale <= 0.9f) {
                if (actualMode.value != ReaderMode.Carousel) {
                    //Log.d("BookContent", "Updating mode from ${actualMode.value} to Carousel")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleModeWrapper(ReaderMode.Carousel, zoomState)
                }
            }

            if (newScale > 1f) {
                if (actualMode.value != ReaderMode.Reading) {
               //     Log.d("BookContent", "Updating mode from ${actualMode.value} to Reading")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleModeWrapper(ReaderMode.Reading, zoomState)
                }
            }
        }

    Box()
    {
        when (mode) {
            ReaderMode.Carousel -> {
                PageCarousel(
                    totalPages = totalPages,
                    initialPage = currentPage,
                    onPageChange = onPageChange,
                    onBack = { onCarouselBack() },
                    applyInversion = documentTheme == com.example.bottomapp.shared.backend.settings.DocThemeMode.Invert,
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    bookContent = getPage,
                    title = title,
                    onSettingsClick = { showSettingsModal.value = !showSettingsModal.value },
                    onChaptersClick = { onToggleMode(ReaderMode.Chapters) },
                    zoomState = handleZoomState,
                    viewerProvider = viewerProvider
                )
                if (showSettingsModal.value) {
                    if (showSettingsModal.value) {
                        SettingsModal(
                            onApplyInversionToggle = {
                                updateDocumentThemeMode(if (it) DocThemeMode.Invert else DocThemeMode.AsIs)
                            },
                            applyInversion = documentTheme == DocThemeMode.Invert,
                            onVerticalScroll = {
                                updateFlipMode(it)
                            },
                            scrollMode = flipMode,
                            onStateChange = { showSettingsModal.value = it }
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
                        initialPage = currentPage,
                        getPage = getPage,
                        zoomState = handleZoomState,
                        viewerProvider = viewerProvider
                    )
                }
            }

            ReaderMode.Chapters -> ChapterScreen(
                onChapterClick = {
                    onPageChange(it)
                    onToggleMode(ReaderMode.Reading)
                },
                tableOfContents = tableOfContent,
                onBack = { onToggleMode(ReaderMode.Carousel) }
            )
        }
    }
}
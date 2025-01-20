package com.example.bottomapp.ui.readerscreens.djvuscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.common.BookContent
import com.example.bottomapp.ui.readerscreens.common.ReaderMode

@Composable
fun DjvuReaderScreen(pdfScreenViewModel: DjvuScreenViewModel) {
    val state by pdfScreenViewModel.djvuScreenStateStateFlow.collectAsState()
    BookContent(
        currentPage = pdfScreenViewModel.currentPage,
        title = pdfScreenViewModel.getTitle(),
        totalPages = pdfScreenViewModel.djvuRenderHelper.pagesCount,
        mode = state.mode,
        onPageChange = { pdfScreenViewModel.changePage(it) },
        onToggleMode = { pdfScreenViewModel.switchReaderMode(it) },
        flipMode = state.curlMode,
        getPage = { pdfScreenViewModel.djvuRenderHelper.openPage(it, state.docThemeMode == DocThemeMode.Invert) },
        documentTheme = state.docThemeMode,
        updateDocumentThemeMode = { pdfScreenViewModel.updateDocumentThemeMode(it) },
        updateFlipMode = { pdfScreenViewModel.updateFlipMode(it) },
        onCarouselBack = { pdfScreenViewModel.switchReaderMode(ReaderMode.Reading) },
        tableOfContent = pdfScreenViewModel.djvuRenderHelper.getTableOfContents(),
        viewerProvider = DjvuViewerProvider()
    )
}
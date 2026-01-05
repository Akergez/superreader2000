package com.akergez.superreader2000.shared.formats.pdf.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.BookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.ReaderMode
import com.akergez.superreader2000.shared.formats.pdf.ui.state.PdfScreenViewModel

@Composable
fun PdfReaderScreen(pdfScreenViewModel: PdfScreenViewModel) {

    val state by pdfScreenViewModel.pdfScreenState.collectAsState()
    BookContent(
        currentPage = pdfScreenViewModel.currentPage,
        title = pdfScreenViewModel.getTitle(),
        totalPages = pdfScreenViewModel.pdfRendererHelper.pagesCount,
        mode = state.mode,
        onPageChange = { pdfScreenViewModel.changePage(it) },
        onToggleMode = { pdfScreenViewModel.switchReaderMode(it) },
        flipMode = state.curlMode,
        getPage = { pdfScreenViewModel.pdfRendererHelper.openPage(it, state.docThemeMode == DocThemeMode.Invert) },
        documentTheme = state.docThemeMode,
        updateDocumentThemeMode = { pdfScreenViewModel.updateDocumentThemeMode(it) },
        updateFlipMode = { pdfScreenViewModel.updateFlipMode(it) },
        onCarouselBack = { pdfScreenViewModel.switchReaderMode(ReaderMode.Reading) },
        tableOfContent = pdfScreenViewModel.pdfRendererHelper.getTableOfContents(),
        viewerProvider = PdfViewerProvider()
    )
}
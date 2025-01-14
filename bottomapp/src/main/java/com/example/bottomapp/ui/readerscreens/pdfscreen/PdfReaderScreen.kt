package com.example.bottomapp.ui.readerscreens.pdfscreen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.bottomapp.ui.readerscreens.common.BookContent
import com.example.bottomapp.ui.readerscreens.common.ReaderMode

@Composable
fun PdfReaderScreen(pdfScreenViewModel: PdfScreenViewModel) {

    val state by pdfScreenViewModel.pdfScreenState.collectAsState()
    BookContent(
        currentPage = state.currentPage,
        title = pdfScreenViewModel.getTitle(),
        totalPages = pdfScreenViewModel.pdfRendererHelper.pagesCount,
        mode = state.mode,
        onPageChange = { pdfScreenViewModel.changePage(it) },
        onToggleMode = { pdfScreenViewModel.switchReaderMode(it) },
        flipMode = state.curlMode,
        getPage = { pdfScreenViewModel.pdfRendererHelper.openPage(it) },
        documentTheme = state.docThemeMode,
        updateDocumentThemeMode = { pdfScreenViewModel.updateDocumentThemeMode(it) },
        updateFlipMode = { pdfScreenViewModel.updateFlipMode(it) },
        onCarouselBack = { pdfScreenViewModel.switchReaderMode(ReaderMode.Reading) },
        tableOfContent = pdfScreenViewModel.pdfRendererHelper.getTableOfContents()
    )
}
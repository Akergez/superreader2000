package com.example.bottomapp.ui.readerscreens.pdfscreen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.bottomapp.ui.readerscreens.common.BookContent

@Composable
fun PdfReaderScreen(pdfScreenViewModel: PdfScreenViewModel) {

    val state by pdfScreenViewModel.pdfScreenState.collectAsState()

    Scaffold { innerPadding ->
        BookContent(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            mode = state.mode,
            onPageChange = { pdfScreenViewModel.changePage(it) },
            onToggleMode = { pdfScreenViewModel.switchReaderMode(it) },
            flipMode = state.curlMode,
            modifier = Modifier.padding(innerPadding),
            getPage = { pdfScreenViewModel.pdfRendererHelper.openPage(it) },
            documentTheme = state.docThemeMode,
            updateDocumentThemeMode = { pdfScreenViewModel.updateDocumentThemeMode(it) }
        )
    }
}
package com.example.bottomapp.ui.readerscreens.pdfscreen

import com.example.bottomapp.ui.readerscreens.common.BookContent
import com.example.bottomapp.backend.RenderHelper.PdfRendererHelper
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.ReaderMode
import org.koin.compose.koinInject

@Composable
fun PdfReaderScreen(book: PdfBookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(ReaderMode.Reading) }

    val context = LocalContext.current
    val pdfFile = remember { book.getFile() }
    val pdfRendererHelper = remember { PdfRendererHelper(context, pdfFile) }
    val totalPages = remember { pdfRendererHelper.getPageCount() }
    val curlMode = remember { settingsManager.getFlipMode() }

    val onPageChange = { newPage: Int ->
        currentPage = newPage
        book.saveProgress(PdfProgress(newPage))
    }

    Scaffold() { innerPadding ->
        BookContent(
            currentPage = currentPage,
            totalPages = totalPages,
            mode = mode,
            renderHelper = pdfRendererHelper,
            onPageChange = onPageChange,
            onToggleMode = { newMode -> mode = newMode },
            settingsManager = settingsManager,
            flipMode = curlMode,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
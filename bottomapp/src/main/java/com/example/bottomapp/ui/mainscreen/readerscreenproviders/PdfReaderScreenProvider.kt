package com.example.bottomapp.ui.mainscreen.readerscreenproviders

import androidx.compose.runtime.Composable
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.BookMetaInfo
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.renderHelper.PdfRendererHelper
import com.example.bottomapp.ui.readerscreens.PdfBookContent
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfReaderScreen
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfScreenViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class PdfReaderScreenProvider(val bookManager: BookManager) :
    IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val pdfBookDomainModel =
            PdfBookDomainModel(updateProgress = { updatedBookLink ->
                bookManager.updateProgress(
                    bookLink.bundleName, updatedBookLink
                )
            }, bookMetaInfo = bookLink.bookMetaInfo, getFileFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow()
            })

        return {
            PdfReaderScreen(
                pdfScreenViewModel = koinViewModel<PdfScreenViewModel>(
                    parameters = {
                        parametersOf(pdfBookDomainModel,
                            PdfRendererHelper { pdfBookDomainModel.getFile() })
                    })
            )
        }
    }
}
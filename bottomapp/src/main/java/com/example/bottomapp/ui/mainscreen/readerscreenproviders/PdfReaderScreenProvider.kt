package com.example.bottomapp.ui.mainscreen.readerscreenproviders

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.domain.PdfBookDomainModel
import com.example.bottomapp.shared.backend.storage.BookStorageEntry

class PdfReaderScreenProvider(val bookManager: BookManager) :
    IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val pdfBookDomainModel =
            PdfBookDomainModel(updateProgress = { updatedBookLink ->
                bookManager.updateProgress(
                    bookLink.bundleName, updatedBookLink
                )
            }, bookMetaInfo = bookLink.bookMetaInfo, getFilePathFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow().toString()
            })

        return {
/*            PdfReaderScreen(
                pdfScreenViewModel = koinViewModel<PdfScreenViewModel>(
                    parameters = {
                        parametersOf(pdfBookDomainModel,
                            PdfRendererHelper { pdfBookDomainModel.getFilePath() })
                    })
            )*/
        }
    }
}
package com.example.bottomapp.shared.ui.screens.mainscreen.readerscreenproviders

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.domain.PdfBookDomainModel
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import com.example.bottomapp.shared.renderHelper.PdfRendererHelper
import com.example.bottomapp.shared.ui.screens.readerscreens.pdfscreen.PdfReaderScreen
import com.example.bottomapp.shared.ui.screens.readerscreens.pdfscreen.PdfScreenViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class PdfReaderScreenProvider(
    val bookManager: BookManager, val settingsManager: ISettingsManager
) : IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val pdfBookDomainModel = PdfBookDomainModel(updateProgress = { updatedBookLink ->
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
                        parametersOf(
                            pdfBookDomainModel,
                            PdfRendererHelper { pdfBookDomainModel.getFile() },
                            settingsManager

                        )
                    })
            )
        }
    }
}
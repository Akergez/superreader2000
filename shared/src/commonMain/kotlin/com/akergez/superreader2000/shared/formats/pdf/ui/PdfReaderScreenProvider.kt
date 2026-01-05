package com.akergez.superreader2000.shared.formats.pdf.ui

import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.formats.pdf.PdfBookDomainModel
import com.akergez.superreader2000.shared.formats.pdf.PdfRendererHelper
import com.akergez.superreader2000.shared.formats.pdf.ui.state.PdfScreenViewModel
import com.akergez.superreader2000.shared.formats.abstractions.IReaderScreenProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class PdfReaderScreenProvider(
    val bookManager: BookManager,
    val settingsManager: ISettingsManager
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
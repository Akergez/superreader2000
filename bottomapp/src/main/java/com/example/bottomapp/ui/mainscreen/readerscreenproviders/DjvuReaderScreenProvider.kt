package com.example.bottomapp.ui.mainscreen.readerscreenproviders

import androidx.compose.runtime.Composable
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.backend.DjvuBookDomainModel
import com.example.bottomapp.backend.DjvuProgress
import com.example.bottomapp.backend.renderHelper.DJVURenderHelper
import com.example.bottomapp.ui.readerscreens.djvuscreen.DjvuReaderScreen
import com.example.bottomapp.ui.readerscreens.djvuscreen.DjvuScreenViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class DjvuReaderScreenProvider(private val bookManager: BookManager) :
    IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val pdfBookDomainModel =
            DjvuBookDomainModel(updateProgress = { updatedBookLink ->
                bookManager.updateProgress(
                    bookLink.bundleName, updatedBookLink
                )
            }, bookMetaInfo = bookLink.bookMetaInfo, getFileFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow()
            })

        return {
            DjvuReaderScreen(
                pdfScreenViewModel = koinViewModel<DjvuScreenViewModel>(
                    parameters = {
                        parametersOf(pdfBookDomainModel,
                            DJVURenderHelper { pdfBookDomainModel.getFile() })
                    })
            )
        }
    }
}
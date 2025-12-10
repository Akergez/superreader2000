package com.example.bottomapp.ui.mainscreen.readerscreenproviders

import Fb2ReaderScreen
import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.domain.Fb2BookDomainModel
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Fb2ReaderScreenProvider(private val bookManager: BookManager) : IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val book = Fb2BookDomainModel(
            bookLink.bookMetaInfo,
            updateProgress = { updatedBookLink ->
                CoroutineScope(Dispatchers.IO).launch {
                    bookManager.updateProgress(
                        bookLink.bundleName, updatedBookLink
                    )
                }
            },
            getFilePathFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow().toString()
            })
        return { Fb2ReaderScreen(book) }
    }
}
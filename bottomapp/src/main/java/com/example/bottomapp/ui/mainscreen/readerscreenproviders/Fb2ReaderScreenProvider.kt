package com.example.bottomapp.ui.mainscreen.readerscreenproviders

import Fb2ReaderScreen
import androidx.compose.runtime.Composable
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Fb2ReaderScreenProvider(private val bookManager: BookManager) : IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val book = Fb2BookDomainModel(bookLink.bookMetaInfo,
            updateProgress = { updatedBookLink ->
                CoroutineScope(Dispatchers.IO).launch {
                    bookManager.updateProgress(
                        bookLink.bundleName, updatedBookLink
                    )
                }
            },
            getFileFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow()
            })
        return { Fb2ReaderScreen(book) }
    }
}
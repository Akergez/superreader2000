package com.akergez.superreader2000.shared.formats.fb2.ui

import Fb2ReaderScreen
import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.formats.fb2.Fb2BookDomainModel
import com.akergez.superreader2000.shared.formats.abstractions.IReaderScreenProvider
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
            getFileFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow()
            })
        return { Fb2ReaderScreen(book) }
    }
}
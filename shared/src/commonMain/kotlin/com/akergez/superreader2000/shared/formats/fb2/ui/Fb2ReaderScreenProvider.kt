package com.akergez.superreader2000.shared.formats.fb2.ui

import Fb2ReaderScreen
import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.formats.fb2.Fb2BookDomainModel
import com.akergez.superreader2000.shared.formats.abstractions.IReaderScreenProvider

class Fb2ReaderScreenProvider(private val bookManager: BookManager) : IReaderScreenProvider {
    override fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit {
        val book = Fb2BookDomainModel(
            bookLink.bookMetaInfo,
            updateProgress = { updatedBookLink ->
                // saveProgress завершается только после обновления общего состояния книги.
                bookManager.updateProgress(
                    bookLink.bundleName, updatedBookLink
                )
            },
            flushProgress = {
                bookManager.saveBookLink(bookLink.bundleName).getOrThrow()
            },
            getFileFromStorage = {
                bookManager.getBookFile(bookLink.bundleName).getOrThrow()
            })
        return { Fb2ReaderScreen(book) }
    }
}

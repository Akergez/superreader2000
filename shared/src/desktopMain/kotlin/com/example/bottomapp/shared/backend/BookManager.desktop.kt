package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.Progress
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import com.example.bottomapp.shared.constants.FileType
import com.sun.jndi.toolkit.url.Uri
import kotlinx.coroutines.flow.StateFlow
import java.io.File

actual class BookManager {
    actual val bookFlow: StateFlow<List<BookStorageEntry>>
        get() = TODO("Not yet implemented")

    actual suspend fun getBookFile(bundleName: String): Result<File> {
        TODO("Not yet implemented")
    }

    actual suspend fun addNewBook(
        fileUri: Uri,
        fileName: String,
        progress: Progress,
        fileType: FileType
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun refreshBooksLinks() {
    }

    actual suspend fun updateProgress(
        bundleName: String,
        bookMetaInfo: BookMetaInfo
    ) {
    }

    actual suspend fun deleteBook(bundleName: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual fun renameBook(
        bundleName: String,
        newNameForBundle: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun updateAuthor(
        bundleName: String,
        author: String?
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun updateBookPreview(
        bundleName: String,
        newPreviewUri: Uri
    ): Result<Uri> {
        TODO("Not yet implemented")
    }

    actual suspend fun saveBookLinks() {
    }
}
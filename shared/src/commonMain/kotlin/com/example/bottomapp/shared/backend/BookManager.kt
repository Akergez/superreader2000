package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.Progress
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import com.example.bottomapp.shared.constants.FileType
import com.example.bottomapp.shared.constants.toFileExtension
import com.sun.jndi.toolkit.url.Uri
import kotlinx.coroutines.flow.StateFlow
import java.io.File

expect class BookManager {
     val bookFlow: StateFlow<List<BookStorageEntry>>

     suspend fun getBookFile(bundleName: String): Result<FileWrapper>

     suspend fun addNewBook(
        fileUri: String, fileName: String, progress: Progress, fileType: FileType
    ): Result<Unit>

     suspend fun refreshBooksLinks()

     suspend fun updateProgress(bundleName: String, bookMetaInfo: BookMetaInfo)

     suspend fun deleteBook(bundleName: String): Result<Unit>

     suspend fun renameBook(bundleName: String, newNameForBundle: String): Result<Unit>

     suspend fun updateAuthor(bundleName: String, author: String?): Result<Unit>

     suspend fun updateBookPreview(bundleName: String, newPreviewUri: String): Result<String>

     suspend fun saveBookLinks()
}
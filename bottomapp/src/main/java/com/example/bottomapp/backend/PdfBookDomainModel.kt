package com.example.bottomapp.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.PdfProgress
import com.example.bottomapp.shared.backend.domain.Progress
import java.io.File

class PdfBookDomainModel(
    val bookMetaInfo: BookMetaInfo,
    val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> File,
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookMetaInfo.title
    private var progress: PdfProgress = bookMetaInfo.progress as? PdfProgress
        ?: PdfProgress(0, 0)

    override fun readProgress(): PdfProgress {
        return progress
    }

    override suspend fun getFile(): File {
        return getFileFromStorage()
    }

    override suspend fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(BookMetaInfo(bookMetaInfo.title, progress, bookMetaInfo.author))
    }
}
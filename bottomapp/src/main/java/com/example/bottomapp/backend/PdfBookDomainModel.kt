package com.example.bottomapp.backend

import java.io.File


data class PdfProgress(val pageIndex: Int, val totalPages: Int?) : Progress(FormatType.PDF) {
    override fun getProgressInPercent(): Double? {
        return if (totalPages != null && totalPages != 0) {
            pageIndex.toDouble() / totalPages.toDouble() * 100
        } else {
            null
        }
    }
}

class PdfBookDomainModel(
    val bookMetaInfo: BookMetaInfo,
    val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> File,
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookMetaInfo.title
    private var progress: PdfProgress = bookMetaInfo.progress as PdfProgress

    override fun readProgress(): PdfProgress {
        return progress
    }

    override suspend fun getFile(): File {
        return getFileFromStorage()
    }

    override suspend fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, progress, bookMetaInfo.author))
    }
}
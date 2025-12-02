package com.example.bottomapp.backend

import java.io.File

data class DjvuProgress(val pageIndex: Int, val totalPages: Int?) :
    Progress(FormatType.DJVU) {
    override fun getProgressInPercent(): Double? {
        return if (totalPages != null && totalPages != 0) {
            pageIndex.toDouble() / totalPages.toDouble() * 100
        } else {
            null
        }
    }
}

class DjvuBookDomainModel(
    val bookMetaInfo: BookMetaInfo,
    val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> File,
) :
    BookDomainModel<DjvuProgress> {
    val title: String = bookMetaInfo.title
    private var progress: DjvuProgress = bookMetaInfo.progress as? DjvuProgress
        ?: DjvuProgress(0, null)

    override fun readProgress(): DjvuProgress {
        return progress
    }

    override suspend fun getFile(): File {
        return getFileFromStorage()
    }

    override suspend fun saveProgress(progress: DjvuProgress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, progress, bookMetaInfo.author))
    }
}
package com.example.bottomapp.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.DjvuProgress
import java.io.File

class DjvuBookDomainModel(
    val bookMetaInfo: BookMetaInfo,
    val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> File,
) :
    BookDomainModel<DjvuProgress> {
    val title: String = bookMetaInfo.title
    private var progress: DjvuProgress = bookMetaInfo.progress as? DjvuProgress
        ?: DjvuProgress(0, 0)

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
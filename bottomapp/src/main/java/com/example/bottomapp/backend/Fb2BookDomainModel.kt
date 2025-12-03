package com.example.bottomapp.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.Fb2Progress
import java.io.File

class Fb2BookDomainModel(
    val bookLink: BookMetaInfo,
    val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> File,
) :
    BookDomainModel<Fb2Progress> {
    val title: String = bookLink.title
    var progress: Fb2Progress = bookLink.progress as? Fb2Progress
        ?: Fb2Progress(0, 0)

    override fun readProgress(): Fb2Progress {
        return progress
    }

    override suspend fun getFile(): File {
        return getFileFromStorage()
    }

    override suspend fun saveProgress(progress: Fb2Progress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, progress, bookLink.author))
    }
}
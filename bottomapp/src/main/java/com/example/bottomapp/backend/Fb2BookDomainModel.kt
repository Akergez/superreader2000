package com.example.bottomapp.backend

import java.io.File


data class Fb2Progress(val wordOffset: Int, val totalWords: Int?) : Progress(FormatType.FB2) {
    override fun getProgressInPercent(): Double? {
        return if (totalWords != null && totalWords != 0) {
            wordOffset.toDouble() / totalWords.toDouble() * 100
        } else {
            null
        }
    }
}

class Fb2BookDomainModel(
    val bookLink: BookMetaInfo,
    val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> File,
) :
    BookDomainModel<Fb2Progress> {
    val title: String = bookLink.title
    var progress: Fb2Progress = bookLink.progress as? Fb2Progress
        ?: Fb2Progress(0, null)

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
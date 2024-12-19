package com.example.bottomapp.backend

import java.io.File


data class Fb2Progress(val wordOffset: Int) : Progress(FormatType.FB2)

class Fb2BookDomainModel(
    bookLink: BookMetaInfo,
    val updateProgress: (BookMetaInfo) -> Unit,
    private val getFileFromStorage: () -> File,
) :
    BookDomainModel<Fb2Progress> {
    val title: String = bookLink.title
    var progress: Fb2Progress = bookLink.progress as Fb2Progress

    override fun readProgress(): Fb2Progress {
        return progress
    }

    override fun getFile(): File {
        return getFileFromStorage()
    }

    override fun saveProgress(progress: Fb2Progress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, progress))
    }
}
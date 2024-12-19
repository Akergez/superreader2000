package com.example.bottomapp.backend

import java.io.File


data class PdfProgress(val pageIndex: Int) : Progress(FormatType.PDF)

class PdfBookDomainModel(
    bookMetaInfo: BookMetaInfo,
    val updateProgress: (BookMetaInfo) -> Unit,
    private val getFileFromStorage: () -> File,
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookMetaInfo.title
    private var progress: PdfProgress = bookMetaInfo.progress as PdfProgress

    override fun readProgress(): PdfProgress {
        return progress
    }

    override fun getFile(): File {
        return getFileFromStorage()
    }

    override fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, progress))
    }
}
package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.formats.abstractions.BookDomainModel
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo

class PdfBookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> FileWrapper,
) : BookDomainModel<PdfProgress> {
    private var progress: PdfProgress = bookMetaInfo.progress as PdfProgress

    override fun readProgress(): PdfProgress = progress

    override suspend fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(
            bookMetaInfo.copy(
                progress = progress
            )
        )
    }

    override suspend fun getFile(): FileWrapper = getFileFromStorage()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}
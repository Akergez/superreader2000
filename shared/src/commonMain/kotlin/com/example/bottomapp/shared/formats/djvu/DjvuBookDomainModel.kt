package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.formats.abstractions.BookDomainModel
import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo

class DjvuBookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> FileWrapper,
) : BookDomainModel<DjvuProgress> {
    private var progress: DjvuProgress = bookMetaInfo.progress as DjvuProgress

    override fun readProgress(): DjvuProgress = progress

    override suspend fun saveProgress(progress: DjvuProgress) {
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
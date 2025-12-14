package com.example.bottomapp.shared.formats.fb2

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.formats.abstractions.BookDomainModel
import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo

class Fb2BookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFileFromStorage: suspend () -> FileWrapper,
) : BookDomainModel<Fb2Progress> {
    private var progress: Fb2Progress = bookMetaInfo.progress as Fb2Progress

    override fun readProgress(): Fb2Progress = progress

    override suspend fun saveProgress(progress: Fb2Progress) {
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


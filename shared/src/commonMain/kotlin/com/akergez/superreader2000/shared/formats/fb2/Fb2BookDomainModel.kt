package com.akergez.superreader2000.shared.formats.fb2

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.formats.abstractions.BookDomainModel
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo

class Fb2BookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val flushProgress: suspend () -> Unit = {},
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

    suspend fun flushProgress() = flushProgress.invoke()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}

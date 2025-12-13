package com.example.bottomapp.shared.backend.domain

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper

interface BookDomainModel<T : Progress> {
    fun readProgress(): T

    suspend fun saveProgress(progress: T)

    suspend fun getFile(): FileWrapper

    fun getMetadata(): BookMetaInfo
}

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
               // djvuProgress = progress
            )
        )
    }

    override suspend fun getFile(): FileWrapper = getFileFromStorage()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}

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
              //  fb2Progress = progress
            )
        )
    }

    override suspend fun getFile(): FileWrapper = getFileFromStorage()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}


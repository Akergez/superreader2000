package com.example.bottomapp.shared.backend.domain

/**
 * Base interface for all book domain models
 * Each book format (PDF, DjVu, FB2) implements this interface
 */
interface BookDomainModel<T : Progress> {
    /**
     * Get the current reading progress
     */
    fun readProgress(): T

    /**
     * Save the reading progress
     */
    suspend fun saveProgress(progress: T)

    /**
     * Get the file path or URI for this book
     */
    suspend fun getFilePath(): String

    /**
     * Get book metadata
     */
    fun getMetadata(): BookMetaInfo
}

/**
 * PDF-specific book domain model implementation
 */
class PdfBookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFilePathFromStorage: suspend () -> String,
) : BookDomainModel<PdfProgress> {
    private var progress: PdfProgress = bookMetaInfo.progress as PdfProgress

    override fun readProgress(): PdfProgress = progress

    override suspend fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(
            bookMetaInfo.copy(
              //  pdfProgress = progress
            )
        )
    }

    override suspend fun getFilePath(): String = getFilePathFromStorage()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}

/**
 * DjVu-specific book domain model implementation
 */
class DjvuBookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFilePathFromStorage: suspend () -> String,
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

    override suspend fun getFilePath(): String = getFilePathFromStorage()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}

/**
 * FB2-specific book domain model implementation
 */
class Fb2BookDomainModel(
    private val bookMetaInfo: BookMetaInfo,
    private val updateProgress: suspend (BookMetaInfo) -> Unit,
    private val getFilePathFromStorage: suspend () -> String,
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

    override suspend fun getFilePath(): String = getFilePathFromStorage()

    override fun getMetadata(): BookMetaInfo = bookMetaInfo
}


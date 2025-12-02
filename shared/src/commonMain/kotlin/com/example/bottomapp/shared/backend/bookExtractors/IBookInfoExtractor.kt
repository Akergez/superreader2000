package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo

/**
 * Interface for extracting book metadata information
 */
interface IBookInfoExtractor<T> {
    suspend fun extract(filePath: String): T
}

/**
 * Platform-specific factory for creating extractors
 */
expect class BookExtractorFactory() {
    fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo>
    fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo>
    fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo>
}


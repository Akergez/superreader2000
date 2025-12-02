package com.example.bottomapp.shared.backend.bookCreators

import com.example.bottomapp.shared.backend.domain.BookMetadata

/**
 * Factory interface for creating book metadata objects from files
 */
interface IBookCreateFactory {
    suspend fun create(filePath: String): BookMetadata?
}

/**
 * Platform-specific factory provider
 */
expect class BookCreatorFactory() {
    fun createPdfFactory(): IBookCreateFactory
    fun createDjvuFactory(): IBookCreateFactory
    fun createFb2Factory(): IBookCreateFactory
}


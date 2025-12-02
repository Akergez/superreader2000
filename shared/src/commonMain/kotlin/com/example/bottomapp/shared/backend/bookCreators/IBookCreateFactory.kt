package com.example.bottomapp.shared.backend.bookCreators

import com.example.bottomapp.shared.backend.domain.BookDomainModel

/**
 * Factory interface for creating book objects from files
 */
interface IBookCreateFactory {
    suspend fun create(filePath: String): BookDomainModel?
}

/**
 * Platform-specific factory provider
 */
expect class BookCreatorFactory() {
    fun createPdfFactory(): IBookCreateFactory
    fun createDjvuFactory(): IBookCreateFactory
    fun createFb2Factory(): IBookCreateFactory
}


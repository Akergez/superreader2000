package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType

/**
 * Factory interface for creating book information extractors
 */

class BookExtractorFactory {
    fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> = PdfBookAuthorExtractor()
    fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> = DjvuBookAuthorExtractor()
    fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> = Fb2BookAuthorExtractor()
}

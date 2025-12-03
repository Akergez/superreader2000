package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType

/**
 * Factory interface for creating book information extractors
 */
interface IBookInfoExtractorFactory {
    fun createExtractor(formatType: FormatType): IBookInfoExtractor<BookMetaInfo>
}

/**
 * Default implementation using BookExtractorFactory
 */
class BookInfoExtractorFactory : IBookInfoExtractorFactory {
    private val factory = BookExtractorFactory()

    override fun createExtractor(formatType: FormatType): IBookInfoExtractor<BookMetaInfo> {
        return when (formatType) {
            FormatType.PDF -> factory.createPdfExtractor()
            FormatType.DJVU -> factory.createDjvuExtractor()
            FormatType.FB2 -> factory.createFb2Extractor()
        }
    }
}


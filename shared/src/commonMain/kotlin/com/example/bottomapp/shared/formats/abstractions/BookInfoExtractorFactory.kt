package com.example.bottomapp.shared.formats.abstractions

import com.example.bottomapp.shared.formats.djvu.DjvuBookAuthorExtractor
import com.example.bottomapp.shared.formats.fb2.Fb2BookAuthorExtractor
import com.example.bottomapp.shared.formats.pdf.PdfBookAuthorExtractor

class BookExtractorFactory {
    fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> =
        PdfBookAuthorExtractor()

    fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> =
        DjvuBookAuthorExtractor()

    fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> =
        Fb2BookAuthorExtractor()
}

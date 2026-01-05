package com.akergez.superreader2000.shared.formats.abstractions

import com.akergez.superreader2000.shared.formats.djvu.DjvuBookAuthorExtractor
import com.akergez.superreader2000.shared.formats.fb2.Fb2BookAuthorExtractor
import com.akergez.superreader2000.shared.formats.pdf.PdfBookAuthorExtractor

class BookExtractorFactory {
    fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> =
        PdfBookAuthorExtractor()

    fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> =
        DjvuBookAuthorExtractor()

    fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> =
        Fb2BookAuthorExtractor()
}

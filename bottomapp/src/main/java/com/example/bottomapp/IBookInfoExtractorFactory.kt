package com.example.bottomapp

import com.example.bottomapp.backend.BookMetaInfo
import com.example.bottomapp.backend.DjvuBookAuthorExtractor
import com.example.bottomapp.backend.DjvuProgress
import com.example.bottomapp.backend.Fb2BookAuthorExtractor
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.IBookInfoExtractor
import com.example.bottomapp.backend.PdfBookAuthorExtractor
import com.example.bottomapp.backend.PdfProgress

interface IBookInfoExtractorFactory<T> {
    fun createExtractor(meta: T): IBookInfoExtractor<T>
}

class BookInfoExtractorFactory : IBookInfoExtractorFactory<BookMetaInfo> {
    override fun createExtractor(meta: BookMetaInfo): IBookInfoExtractor<BookMetaInfo> {
        return when (meta.progress) {
            is PdfProgress -> PdfBookAuthorExtractor()
            is Fb2Progress -> Fb2BookAuthorExtractor()
            is DjvuProgress -> DjvuBookAuthorExtractor()
            else -> throw IllegalArgumentException("Unknown progress type")
        }
    }
}
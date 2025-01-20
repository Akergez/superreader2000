package com.example.bottomapp

import com.example.bottomapp.backend.BookMetaInfo
import com.example.bottomapp.backend.DjvuBookAuthorExtractor
import com.example.bottomapp.backend.DjvuProgress
import com.example.bottomapp.backend.Fb2BookAuthorExtractor
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.backend.IBookInfoExtractor
import com.example.bottomapp.backend.PdfBookAuthorExtractor
import com.example.bottomapp.backend.PdfProgress

interface IBookInfoExtractorFactory<T> {
    fun createExtractor(meta: T): IBookInfoExtractor<T>
}

class BookInfoExtractorFactory(private val instancesMap: Map<FormatType, IBookInfoExtractor<BookMetaInfo>>) :
    IBookInfoExtractorFactory<BookMetaInfo> {
    override fun createExtractor(meta: BookMetaInfo): IBookInfoExtractor<BookMetaInfo> {
        return instancesMap[meta.progress.formatType] ?: throw NotImplementedError(
            "Unknown format type ${meta.progress.formatType}," +
                    " can not resolve IBookInfoExtractor implementation" +
                    " check your DI configuration"
        )
    }
}
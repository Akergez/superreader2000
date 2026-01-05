package com.akergez.superreader2000.shared.formats.abstractions

interface IBookInfoExtractorFactory<T> {
    fun createExtractor(meta: T): IBookInfoExtractor<T>
}

class BookInfoExtractorFactory(private val instancesMap: Map<FormatType, IBookInfoExtractor<BookMetaInfo>>) :
    IBookInfoExtractorFactory<BookMetaInfo> {
    override fun createExtractor(meta: BookMetaInfo): IBookInfoExtractor<BookMetaInfo> {
        return instancesMap[meta.progress!!.formatType] ?: throw NotImplementedError(
            "Unknown format type ${meta.progress!!.formatType}," +
                    " can not resolve IBookInfoExtractor implementation" +
                    " check your DI configuration"
        )
    }
}
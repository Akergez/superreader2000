package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.storage.IDeserializer
import com.example.bottomapp.shared.backend.storage.INameProvider
import com.example.bottomapp.shared.backend.storage.StorageManager
import com.example.bottomapp.shared.backend.bookExtractors.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.settings.ISettingsManager

class StorageAdapter() : IDeserializer<BookMetaInfo>,
    INameProvider<BookMetaInfo> {
    override fun deserialize(jsonString: String): BookMetaInfo {
        return BookMetaInfo("", null, null)
    }

    override fun serialize(obj: BookMetaInfo): String {
        return ""
    }

    override fun jsonMetaFilename(bundleName: String): String {
        return "${bundleName}_meta.json"
    }

    override fun previewFilename(bundleName: String): String {
        return ".${bundleName}_preview.jpg"
    }

    override fun fileName(bundleName: String, meta: BookMetaInfo): String {
        return when (meta.progress!!.formatType) {
            FormatType.PDF -> "${bundleName}.pdf"
            FormatType.FB2 -> "${bundleName}.fb2"
            FormatType.DJVU -> "${bundleName}.djvu"
        }
    }
}

expect class BookStorage : StorageManager<BookMetaInfo>
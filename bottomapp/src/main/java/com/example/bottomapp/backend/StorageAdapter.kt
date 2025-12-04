package com.example.bottomapp.backend

import android.content.Context
import com.example.bottomapp.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.backend.storage.IDeserializer
import com.example.bottomapp.backend.storage.INameProvider
import com.example.bottomapp.backend.storage.StorageManager
import com.example.bottomapp.shared.backend.bookExtractors.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.koin.compose.getKoin
import org.koin.compose.koinInject

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

class BookStorage(
    context: Context,
    settingsManager: SettingsManager,
    adapter: StorageAdapter,
    infoExtractorFactory: BookInfoExtractorFactory,
    previewGeneratorMap: Map<FormatType, IPreviewGenerator>
) : StorageManager<BookMetaInfo>(
    context,
    settingsManager,
    adapter,
    adapter,
    infoExtractorFactory,
    {
        previewGeneratorMap[it.progress!!.formatType]
            ?: throw NotImplementedError("PreviewGenerator for ${it.progress!!.formatType} not found, check your DI config")
    }
)
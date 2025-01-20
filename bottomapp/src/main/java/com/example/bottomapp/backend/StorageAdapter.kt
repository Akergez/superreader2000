package com.example.bottomapp.backend

import android.content.Context
import com.example.bottomapp.BookInfoExtractorFactory
import com.example.bottomapp.backend.storage.IDeserializer
import com.example.bottomapp.backend.storage.INameProvider
import com.example.bottomapp.backend.storage.StorageManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class StorageAdapter : IDeserializer<BookMetaInfo>, INameProvider<BookMetaInfo> {

    private val progressAdapterFactory = RuntimeTypeAdapterFactory.of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")
        .registerSubtype(Fb2Progress::class.java, "fb2")
        .registerSubtype(DjvuProgress::class.java, "djvu")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()

    override fun deserialize(jsonString: String): BookMetaInfo {
        return gson.fromJson(jsonString, BookMetaInfo::class.java)
    }

    override fun serialize(obj: BookMetaInfo): String {
        return gson.toJson(obj)
    }

    override fun jsonMetaFilename(bundleName: String): String {
        return "${bundleName}_meta.json"
    }

    override fun previewFilename(bundleName: String): String {
        return ".${bundleName}_preview.jpg"
    }

    override fun fileName(bundleName: String, meta: BookMetaInfo): String {
        return when (meta.progress) {
            is PdfProgress -> "${bundleName}.pdf"
            is Fb2Progress -> "${bundleName}.fb2"
            is DjvuProgress -> "${bundleName}.djvu"
            else -> {
                throw IllegalArgumentException("Unknown progress type")
            }
        }
    }
}

class BookStorage(
    context: Context,
    settingsManager: SettingsManager,
    adapter: StorageAdapter,
    infoExtractorFactory: BookInfoExtractorFactory
) : StorageManager<BookMetaInfo>(context, settingsManager, adapter, adapter, infoExtractorFactory)
package com.akergez.superreader2000.shared.backend.storage

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import kotlinx.serialization.json.Json

class BookMetaInfoDeserializer(private val json: Json) : IDeserializer<BookMetaInfo>,
    INameProvider<BookMetaInfo> {

    override fun deserialize(jsonString: String): BookMetaInfo {
        return try {
            json.decodeFromString<BookMetaInfo>(jsonString)
        } catch (e: Exception) {
            BookMetaInfo()
        }
    }

    override fun serialize(obj: BookMetaInfo): String {
        return json.encodeToString(BookMetaInfo.serializer(), obj)
    }

    override fun jsonMetaFilename(bundleName: String): String {
        return "${bundleName}_meta.json"
    }

    override fun previewFilename(bundleName: String): String {
        return ".${bundleName}_preview.jpg"
    }

    override fun fileName(bundleName: String, meta: BookMetaInfo): String {
        return "$bundleName.pdf"
    }
}
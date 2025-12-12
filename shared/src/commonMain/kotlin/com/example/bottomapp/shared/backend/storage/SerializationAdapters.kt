package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.serialization.json.Json

/**
 * Kotlinx Serialization adapter for BookMetaInfo
 */
class BookMetaInfoDeserializer : IDeserializer<BookMetaInfo> {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun deserialize(jsonString: String): BookMetaInfo {
        return try {
            json.decodeFromString<BookMetaInfo>(jsonString)
        } catch (e: Exception) {
            // Fallback to default if parsing fails
            BookMetaInfo()
        }
    }

    override fun serialize(obj: BookMetaInfo): String {
        return json.encodeToString(BookMetaInfo.serializer(), obj)
    }
}
class BookMetaInfoNameProvider : INameProvider<BookMetaInfo> {
    override fun jsonMetaFilename(bundleName: String): String {
        return "${bundleName}_meta.json"
    }

    override fun previewFilename(bundleName: String): String {
        return ".${bundleName}_preview.jpg"
    }

    override fun fileName(bundleName: String, meta: BookMetaInfo): String {
        // Determine file extension based on format type
        return "$bundleName.pdf" // Default to PDF, can be enhanced based on meta.formatType if available
    }
}


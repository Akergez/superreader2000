package com.example.bottomapp.shared.backend.domain

import kotlinx.serialization.Serializable

enum class FormatType {
    PDF,
    DJVU,
    FB2
}

enum class FileType {
    pdf,
    fb2,
    djvu,
    unknown
}

fun FileType.toFileExtension(): String {
    return when (this) {
        FileType.pdf -> "pdf"
        FileType.fb2 -> "fb2"
        FileType.djvu -> "djvu"
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}

fun FileType.toMimeType(): List<String> {
    return when (this) {
        FileType.pdf -> listOf("application/pdf")
        FileType.fb2 -> listOf("application/fb2", "text/fb2+xml")
        FileType.djvu -> listOf("application/djvu")
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}

@Serializable
sealed class BookMetadata {
    abstract val uri: String
    abstract val name: String
    abstract val formatType: FormatType
    abstract val pageCount: Int
}

@Serializable
data class PdfBookMetadata(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.PDF,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = "",
    val subject: String = "",
    val producer: String = ""
) : BookMetadata()

@Serializable
data class DjvuBookMetadata(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.DJVU,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = ""
) : BookMetadata()

@Serializable
data class Fb2BookMetadata(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.FB2,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = "",
    val lang: String = "en"
) : BookMetadata()

@Serializable
data class BookMetaInfo(
    val title: String = "",
    val progress: Progress? = null,
    val author: String? = null,
)

@Serializable
sealed class Progress {
    abstract fun getProgressInPercent(): Double?

    abstract val formatType: FormatType;
}

@Serializable
data class PdfProgress(
    val currentPage: Int,
    val totalPages: Int
) : Progress() {
    override fun getProgressInPercent(): Double {
        if (totalPages == 0) {
            return 0.0
        }
        return currentPage / totalPages.toDouble() * 100
    }

    override val formatType: FormatType = FormatType.PDF
}

@Serializable
data class DjvuProgress(
    val currentPage: Int,
    val totalPages: Int
) : Progress() {
    override val formatType: FormatType = FormatType.DJVU

    override fun getProgressInPercent(): Double {
        if (totalPages == 0) {
            return 0.0
        }
        return currentPage / totalPages.toDouble() * 100
    }
}

@Serializable
data class Fb2Progress(
    val characterPosition: Int = 0,
    val totalCharacters: Int = 0
) : Progress() {
    override fun getProgressInPercent(): Double {
        return characterPosition / totalCharacters.toDouble() * 100
    }

    override val formatType: FormatType = FormatType.FB2
}


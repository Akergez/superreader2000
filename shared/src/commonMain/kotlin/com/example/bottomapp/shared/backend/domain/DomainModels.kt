package com.example.bottomapp.shared.backend.domain

import kotlinx.serialization.Serializable

/**
 * Format type enumeration for different document formats
 */
enum class FormatType {
    PDF,
    DJVU,
    FB2
}

/**
 * File type enumeration for file extensions and MIME types
 */
enum class FileType {
    pdf,
    fb2,
    djvu,
    unknown
}

/**
 * Convert FileType to file extension
 */
fun FileType.toFileExtension(): String {
    return when (this) {
        FileType.pdf -> "pdf"
        FileType.fb2 -> "fb2"
        FileType.djvu -> "djvu"
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}

/**
 * Convert FileType to MIME types
 */
fun FileType.toMimeType(): List<String> {
    return when (this) {
        FileType.pdf -> listOf("application/pdf")
        FileType.fb2 -> listOf("application/fb2", "text/fb2+xml")
        FileType.djvu -> listOf("application/djvu")
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}

/**
 * Base domain model for books (metadata container)
 */
@Serializable
sealed class BookMetadata {
    abstract val uri: String
    abstract val name: String
    abstract val formatType: FormatType
    abstract val pageCount: Int
}

/**
 * PDF-specific book metadata
 */
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

/**
 * DjVu-specific book metadata
 */
@Serializable
data class DjvuBookMetadata(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.DJVU,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = ""
) : BookMetadata()

/**
 * FB2-specific book metadata
 */
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

/**
 * Book metadata information with progress tracking
 */
@Serializable
data class BookMetaInfo(
    val author: String = "",
    val title: String = "",
    val subject: String = "",
    val pageCount: Int = 0,
    val producer: String = "",
    val pdfProgress: PdfProgress? = null,
    val djvuProgress: DjvuProgress? = null,
    val fb2Progress: Fb2Progress? = null
)

/**
 * Progress tracking for reading
 */
@Serializable
sealed class Progress {
    abstract val currentPage: Int
    abstract val totalPages: Int
}

@Serializable
data class PdfProgress(
    override val currentPage: Int,
    override val totalPages: Int,
    val zoom: Float = 1f
) : Progress()

@Serializable
data class DjvuProgress(
    override val currentPage: Int,
    override val totalPages: Int,
    val zoom: Float = 1f
) : Progress()

@Serializable
data class Fb2Progress(
    override val currentPage: Int,
    override val totalPages: Int,
    val characterPosition: Int = 0
) : Progress()


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
 * File type enumeration
 */
enum class FileType {
    pdf,
    djvu,
    fb2
}

/**
 * Base domain model for books
 */
@Serializable
sealed class BookDomainModel {
    abstract val uri: String
    abstract val name: String
    abstract val formatType: FormatType
    abstract val pageCount: Int
}

/**
 * PDF-specific book model
 */
@Serializable
data class PdfBookDomainModel(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.PDF,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = "",
    val subject: String = "",
    val producer: String = ""
) : BookDomainModel()

/**
 * DjVu-specific book model
 */
@Serializable
data class DjvuBookDomainModel(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.DJVU,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = ""
) : BookDomainModel()

/**
 * FB2-specific book model
 */
@Serializable
data class Fb2BookDomainModel(
    override val uri: String,
    override val name: String,
    override val formatType: FormatType = FormatType.FB2,
    override val pageCount: Int = 0,
    val author: String = "",
    val title: String = "",
    val lang: String = "en"
) : BookDomainModel()

/**
 * Book metadata information
 */
@Serializable
data class BookMetaInfo(
    val author: String = "",
    val title: String = "",
    val subject: String = "",
    val pageCount: Int = 0,
    val producer: String = ""
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


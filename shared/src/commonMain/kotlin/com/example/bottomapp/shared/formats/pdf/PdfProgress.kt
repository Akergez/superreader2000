package com.example.bottomapp.shared.formats.pdf

import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.Progress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("pdf")
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

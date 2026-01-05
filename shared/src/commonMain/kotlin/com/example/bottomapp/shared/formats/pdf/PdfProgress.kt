package com.example.bottomapp.shared.formats.pdf

import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.Progress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PDF")
data class PdfProgress(
    val pageIndex: Int,
    val totalPages: Int
) : Progress() {
    override fun getProgressInPercent(): Double {
        if (totalPages == 0) {
            return 0.0
        }
        return kotlin.math.round(pageIndex / totalPages.toDouble() * 100)
    }

    override val formatType: FormatType = FormatType.PDF
}

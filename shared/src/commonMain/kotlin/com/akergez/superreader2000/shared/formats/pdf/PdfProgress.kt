package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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

    @Transient
    override val formatType: FormatType = FormatType.PDF
}

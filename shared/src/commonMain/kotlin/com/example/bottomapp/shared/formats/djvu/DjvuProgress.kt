package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.Progress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("DJVU")
data class DjvuProgress(
    val currentPage: Int,
    val totalPages: Int?
) : Progress() {
    override val formatType: FormatType = FormatType.DJVU

    override fun getProgressInPercent(): Double {
        if (totalPages == 0) {
            return 0.0
        }
        return currentPage / (totalPages?.toDouble() ?: 1.0) * 100
    }
}
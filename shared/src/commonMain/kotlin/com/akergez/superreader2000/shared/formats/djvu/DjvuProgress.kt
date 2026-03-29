package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("DJVU")
data class DjvuProgress(
    val currentPage: Int,
    val totalPages: Int?
) : Progress() {
    @Transient
    override val formatType: FormatType = FormatType.DJVU

    override fun getProgressInPercent(): Double {
        if (totalPages == 0) {
            return 0.0
        }
        return currentPage / (totalPages?.toDouble() ?: 1.0) * 100
    }
}
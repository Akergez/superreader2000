package com.akergez.superreader2000.shared.formats.fb2

import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("FB2")
data class Fb2Progress(
    val characterPosition: Int = 0,
    val totalCharacters: Int = 0
) : Progress() {
    override fun getProgressInPercent(): Double {
        return characterPosition / totalCharacters.toDouble() * 100
    }

    override val formatType: FormatType = FormatType.FB2
}

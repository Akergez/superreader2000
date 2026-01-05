package com.example.bottomapp.shared.formats.abstractions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("formatType")
abstract class Progress {
    abstract fun getProgressInPercent(): Double?

    abstract val formatType: FormatType
}

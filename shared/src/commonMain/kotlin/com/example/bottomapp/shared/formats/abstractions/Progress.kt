package com.example.bottomapp.shared.formats.abstractions

import kotlinx.serialization.Serializable

@Serializable
abstract class Progress {
    abstract fun getProgressInPercent(): Double?

    abstract val formatType: FormatType;
}

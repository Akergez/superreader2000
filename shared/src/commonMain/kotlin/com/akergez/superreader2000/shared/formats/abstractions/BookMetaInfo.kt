package com.akergez.superreader2000.shared.formats.abstractions

import kotlinx.serialization.Serializable

@Serializable
data class BookMetaInfo(
    val title: String = "",
    val progress: Progress? = null,
    val author: String? = null,
)

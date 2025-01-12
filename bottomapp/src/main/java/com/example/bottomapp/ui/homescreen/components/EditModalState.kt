package com.example.bottomapp.ui.homescreen.components

import android.net.Uri

const val NotSupportedMessage = "Not supported"

data class EditModalState (
    val previewUri: Uri? = null,
    val title: String = "",
    val author: String = NotSupportedMessage,
    val changed: Boolean = false,
    val previewChanged: Boolean = false,
    val isDragging: Boolean = false
)
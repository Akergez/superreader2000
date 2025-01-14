package com.example.bottomapp.ui.homescreen.components

import android.net.Uri

data class EditModalState (
    val previewUri: Uri? = null,
    val title: String = "",
    val author: String = "",
    val changed: Boolean = false,
    val previewChanged: Boolean = false,
    val isDragging: Boolean = false
)
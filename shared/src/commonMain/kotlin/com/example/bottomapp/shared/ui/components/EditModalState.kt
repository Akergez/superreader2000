package com.example.bottomapp.shared.ui.components

data class EditModalState (
    val previewUri: String? = null,
    val title: String = "",
    val author: String = "",
    val changed: Boolean = false,
    val previewChanged: Boolean = false,
    val isDragging: Boolean = false
)
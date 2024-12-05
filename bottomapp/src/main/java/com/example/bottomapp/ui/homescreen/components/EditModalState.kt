package com.example.bottomapp.ui.homescreen.components

import android.net.Uri

data class EditModalState (
    val previewUri: Uri? = null,
    val title: String = "",
    val author: String = "Не поддерживается"
)
package com.example.bottomapp.ui.homescreen.components

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.bottomapp.backend.BookStorageEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditModalViewModel(val book: BookStorageEntry): ViewModel() {
    private val _editModalState = MutableStateFlow(EditModalState())
    val editModalState: StateFlow<EditModalState> = _editModalState.asStateFlow()

    init {
        _editModalState.value = EditModalState(book.previewUri, book.bookMetaInfo.title, "Не поддерживается")
    }

    fun initState(book: BookStorageEntry) {
        _editModalState.value = EditModalState(book.previewUri, book.bookMetaInfo.title, "Не поддерживается")
    }

    fun updateUri(previewUri: Uri) {
        _editModalState.value = _editModalState.value.copy(previewUri = previewUri, changed = true)
    }

    fun updateTitle(title: String) {
        _editModalState.value = _editModalState.value.copy(title = title, changed = true)
    }

    fun updateAuthor(author: String) {
        _editModalState.value = _editModalState.value.copy(author = author, changed = true)
    }
}
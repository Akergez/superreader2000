package com.example.bottomapp.ui.homescreen.components

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditModalViewModel(val book: BookStorageEntry): ViewModel() {
    private val _editModalState = MutableStateFlow(EditModalState())
    val editModalState: StateFlow<EditModalState> = _editModalState.asStateFlow()

    init {
        _editModalState.value = EditModalState(book.previewUri!!.toUri(), book.bookMetaInfo.title, book.bookMetaInfo.author ?: "")
    }

    fun updateTitle(title: String) {
        _editModalState.value = _editModalState.value.copy(title = title, changed = true)
    }

    fun updateAuthor(author: String) {
        _editModalState.value = _editModalState.value.copy(author = author, changed = true)
    }

    fun uploadImage(uri: Uri) {
        _editModalState.value = _editModalState.value.copy(previewUri = uri, previewChanged = true, changed = true)
    }
}
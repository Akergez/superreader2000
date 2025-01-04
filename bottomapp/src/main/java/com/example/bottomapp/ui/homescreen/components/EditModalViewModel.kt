package com.example.bottomapp.ui.homescreen.components

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.bottomapp.backend.BookStorageEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditModalViewModel(val book: BookStorageEntry): ViewModel() {
    private val _editModalState = MutableStateFlow(EditModalState())
    private val vibrationThresholdHandler = ThresholdHandler(
        threshold = 0.3f,
        updateDraggingAfterExceeding = false)
    private val upperStripThresholdHandler = ThresholdHandler(threshold = 1f)

    val editModalState: StateFlow<EditModalState> = _editModalState.asStateFlow()

    init {
        _editModalState.value = EditModalState(book.previewUri, book.bookMetaInfo.title, NotSupportedMessage)
    }

    fun initState(book: BookStorageEntry) {
        _editModalState.value = EditModalState(book.previewUri, book.bookMetaInfo.title, NotSupportedMessage)
    }

    fun resetState(book: BookStorageEntry) {
        _editModalState.value = EditModalState(book.previewUri, book.bookMetaInfo.title, "Not supported")
    }

    fun updateDragging(value: Boolean) {
        _editModalState.value = _editModalState.value.copy(isDragging = value)
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

    fun handleVibrationFeedback(value: Float, onExceeding: () -> Unit, onLowering: () -> Unit) {
        vibrationThresholdHandler.update(value, onExceeding, onLowering)
    }

    fun handleUpperStrip(value: Float, onExceeding: () -> Unit, onLowering: () -> Unit) {
        upperStripThresholdHandler.update(value, onExceeding, onLowering)
    }
}


class ThresholdHandler(
    private val threshold: Float,
    private var updateDraggingAfterLowering:Boolean = true,
    private var updateDraggingAfterExceeding: Boolean = true) {

    fun update(value: Float, onExceeding: () -> Unit, onLowering: () -> Unit) {
        if (value < threshold) {
            if (updateDraggingAfterLowering){
                onLowering()
                updateDraggingAfterExceeding = true
            }
            updateDraggingAfterLowering = false
        } else {
            if (updateDraggingAfterExceeding) {
                onExceeding()
                updateDraggingAfterLowering = true
            }
            updateDraggingAfterExceeding = false
        }
    }
}

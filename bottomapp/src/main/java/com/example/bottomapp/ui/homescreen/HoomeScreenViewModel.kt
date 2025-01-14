package com.example.bottomapp.ui.homescreen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.exceptions.ExceptionsCatcher
import com.example.bottomapp.constants.FileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HoomeScreenViewModel(
    private val bookManager: BookManager
) : ViewModel() {

    private val _hoomeScreenState = MutableStateFlow(HoomeScreenState())
    val hoomeScreenState: StateFlow<HoomeScreenState> = _hoomeScreenState.asStateFlow()
    private val _books = MutableStateFlow(emptyList<BookStorageEntry>())
    val selectedBooks = MutableStateFlow(emptyList<BookStorageEntry>())
    val books = _books

    init {
        viewModelScope.launch {
            _hoomeScreenState.value = HoomeScreenState()
            bookManager.saveBookLinks()
            bookManager.bookFlow.collect {
                _books.value = it
            }
        }
    }

    fun dumpBooks() {
        viewModelScope.launch {
            bookManager.saveBookLinks()
        }
    }

    fun addBook(uri: Uri, context: Context) {
        val fileName = getFileName(context, uri)
        when (val fileType = getFileType(fileName)) {
            FileType.fb2 -> {
                val bookLink = Fb2Progress(0, null)
                viewModelScope.launch {
                    bookManager.addNewBook(uri, fileName, bookLink, fileType)
                }
            }

            FileType.pdf -> {
                val bookLink = PdfProgress(0, null)
                viewModelScope.launch {
                    bookManager.addNewBook(uri, fileName, bookLink, fileType)
                        .onFailure { ExceptionsCatcher.makeToastByThrowable(context, it) }
                }
            }

            FileType.unknown -> {
                Toast.makeText(
                    context,
                    "Только файлы формата PDF и FB2 поддерживаются приложением",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun updateSelectedBook(book: BookStorageEntry) {
        if (book in selectedBooks.value) {
            selectedBooks.value = selectedBooks.value.filter { x -> x != book }
        } else {
            selectedBooks.value = selectedBooks.value
            selectedBooks.update { currentList ->
                currentList.toMutableList().apply {
                    add(book)
                }
            }
        }
    }

    fun clearSelectedBooks() {
        selectedBooks.value = emptyList()
    }

    fun updateEditingBook(book: BookStorageEntry?) {
        _hoomeScreenState.value = _hoomeScreenState.value.copy(editingBook = book)
    }

    fun clearTemporaryState() {
        _hoomeScreenState.value =
            _hoomeScreenState.value.copy(editingBook = null)
        clearSelectedBooks()
    }

    fun onSaveEditModal(
        oldBook: BookStorageEntry,
        newBook: BookStorageEntry,
        changed: Boolean,
        previewChanged: Boolean,
        context: Context
    ) {
        if (changed) {
            handleBookUpdate(oldBook, newBook, previewChanged, context)
        }
        updateEditingBook(null)
    }

    private fun handleBookUpdate(
        oldBook: BookStorageEntry,
        newBook: BookStorageEntry,
        previewChanged: Boolean,
        context: Context
    ) {
        var updatedBook = newBook
        if (previewChanged) {
            viewModelScope.launch {
                bookManager.updateBookPreview(oldBook.bundleName, newBook.previewUri!!).onSuccess {
                    updatedBook = newBook.copy(previewUri = it)
                }
            }
        }
        if (newBook.bundleName != oldBook.bundleName) {
            viewModelScope.launch {
                bookManager.renameBook(
                    oldBook.bundleName, updatedBook.bundleName
                ).onFailure {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }.onSuccess {
                    if (newBook.bookMetaInfo.author != oldBook.bookMetaInfo.author) {
                        viewModelScope.launch {
                            bookManager.updateAuthor(
                                newBook.bundleName,
                                newBook.bookMetaInfo.author
                            )
                        }
                    }
                }
            }
            return
        }

        if (newBook.bookMetaInfo.author != oldBook.bookMetaInfo.author) {
            viewModelScope.launch {
                bookManager.updateAuthor(newBook.bundleName, newBook.bookMetaInfo.author)
            }
        }
    }

    fun onDeleteBooks(
        books: List<BookStorageEntry>,
        context: Context
    ) {
        viewModelScope.launch {
            books.map { book ->
                bookManager.deleteBook(book.bundleName)
                    .onFailure {
                        Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                    }
            }
        }
        clearSelectedBooks()
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unknown"
}

private fun getFileType(fileName: String): FileType {
    val strType = fileName.substringAfterLast('.', "").lowercase()
    val fileTypeEnum: FileType = try {
        enumValueOf<FileType>(strType)
    } catch (e: IllegalArgumentException) {
        FileType.unknown
    }
    return fileTypeEnum
}
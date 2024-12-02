package com.example.bottomapp.ui.homescreen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.exceptions.ExceptionsCatcher
import com.example.bottomapp.constants.FileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HoomeScreenViewModel(
    private val bookStorage: BookStorage
) : ViewModel() {

    private val _hoomeScreenState = MutableStateFlow(HoomeScreenState())
    val hoomeScreenState: StateFlow<HoomeScreenState> = _hoomeScreenState.asStateFlow()

    init {
        viewModelScope.launch {
            val books = bookStorage.bookCache
            _hoomeScreenState.value = HoomeScreenState(books = books)
            bookStorage.saveBooksLinksToStorage()
        }
    }

    fun addBook(uri: Uri, context: Context) {
        val fileName = getFileName(context, uri)
        when (val fileType = getFileType(fileName)) {
            FileType.fb2 -> {
                val bookLink = Fb2Progress(0)
                bookStorage.addNewBook(uri, fileName, bookLink, fileType)
            }

            FileType.pdf -> {
                val bookLink = PdfProgress(0)
                bookStorage.addNewBook(uri, fileName, bookLink, fileType)
                    .onFailure { ExceptionsCatcher.makeToastByThrowable(context, it) }
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
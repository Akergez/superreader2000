package com.example.bottomapp.backend

import android.content.Context
import com.google.gson.Gson
import java.io.File


data class PdfProgress(val pageIndex: Int) : Progress(FormatType.PDF)

class PdfBookDomainModel(
    bookLink: BookLink,
    val context: Context,
    val updateProgress: (BookLink) -> Unit,
    val storageManager: StorageManager
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookLink.title
    val fileName: String = bookLink.fileName
    var progress: PdfProgress = bookLink.progress as PdfProgress

    private val gson = Gson()

    override fun readProgress(): PdfProgress {
        return progress;
    }

    override fun getFile(): File {
        return storageManager.getBookFile(fileName)
    }

    override fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(BookLink(title, fileName, progress))
    }
}
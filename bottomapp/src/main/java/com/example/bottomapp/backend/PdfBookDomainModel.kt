package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import java.io.File
import java.io.FileReader


data class PdfProgress(val pageIndex: Int) : Progress(FormatType.PDF)

class PdfBookDomainModel(
    bookLink: BookLink,
    val context: Context,
    val updateProgress: (BookLink) -> Unit,
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookLink.title
    val filePath: String = bookLink.filePath
    var progress: PdfProgress = bookLink.progress as PdfProgress

    private val gson = Gson()

    override fun readProgress(): PdfProgress {
        return progress;
    }

    override fun getFile(): File {
        return File(filePath)
    }

    override fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(BookLink(title, filePath, progress))
    }
}
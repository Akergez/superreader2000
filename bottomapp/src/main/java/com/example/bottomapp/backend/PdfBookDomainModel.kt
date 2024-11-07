package com.example.bottomapp.backend

import android.content.Context
import com.google.gson.Gson
import java.io.File


data class PdfProgress(val pageIndex: Int) : Progress(FormatType.PDF)

class PdfBookDomainModel(
    bookMetaInfo: BookMetaInfo,
    val context: Context,
    val updateProgress: (BookMetaInfo) -> Unit,
    val storageManager: StorageManager
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookMetaInfo.title
    val fileName: String = bookMetaInfo.fileName
    var progress: PdfProgress = bookMetaInfo.progress as PdfProgress

    override fun readProgress(): PdfProgress {
        return progress;
    }

    override fun getFile(): File {
        return storageManager.getBookFile(fileName)
    }

    override fun saveProgress(progress: PdfProgress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, fileName, progress))
    }
}
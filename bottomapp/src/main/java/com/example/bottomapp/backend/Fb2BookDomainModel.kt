package com.example.bottomapp.backend

import android.content.Context
import com.google.gson.Gson
import java.io.File


data class Fb2Progress(val wordOffset: Int) : Progress(FormatType.FB2)

class Fb2BookDomainModel(
    bookLink: BookMetaInfo,
    val context: Context,
    val updateProgress: (BookMetaInfo) -> Unit,
    val storageManager: StorageManager
) :
    BookDomainModel<Fb2Progress> {
    val title: String = bookLink.title
    val fileName: String = bookLink.fileName
    var progress: Fb2Progress = bookLink.progress as Fb2Progress

    override fun readProgress(): Fb2Progress {
        return progress;
    }

    override fun getFile(): File {
        return storageManager.getBookFile(fileName).getOrThrow()
    }

    override fun saveProgress(progress: Fb2Progress) {
        this.progress = progress
        updateProgress(BookMetaInfo(title, fileName, progress))
    }
}
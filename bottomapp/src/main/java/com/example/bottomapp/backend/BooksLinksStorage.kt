package com.example.bottomapp.backend

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class BookLink(val fileName: String, val title: String, val progress: Progress)

class BooksLinksStorage(context: Context, val storageManager: StorageManager) {

    private val progressAdapterFactory = RuntimeTypeAdapterFactory
        .of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()
    val booksLinks: SnapshotStateList<BookLink> = mutableStateListOf()

    init {
        booksLinks.addAll(getBooksLinksFromStorage())
    }

    private fun getBooksLinksFromStorage(): List<BookLink> {
        return storageManager.listAllBooks();
    }

    fun saveBooksLinksToStorage() {
        booksLinks.map { bookLink -> storageManager.updateProgressData(bookLink) }
    }

    fun upsertBook(bookLink: BookLink) {
        booksLinks.removeAll { it.fileName == bookLink.fileName }
        booksLinks.add(bookLink)
    }
}
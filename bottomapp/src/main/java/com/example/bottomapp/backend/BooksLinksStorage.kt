package com.example.bottomapp.backend

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class BookLink(val fileName: String, val title: String, val progress: Progress)

class BooksLinksStorage(context: Context, val storageManager: StorageManager) {

    private val progressAdapterFactory = RuntimeTypeAdapterFactory
        .of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()
    val booksLinks: SnapshotStateList<BookLink> = mutableStateListOf()
    val booksHashMap: HashSet<String> = HashSet();

    init {
        GlobalScope.launch {
            refreshBooksLinks()
        }
    }

    private suspend fun getBooksLinksFromStorage(): List<BookLink> {
        return storageManager.listAllBooks();
    }

    suspend fun refreshBooksLinks() {
        booksLinks.clear()
        booksLinks.addAll(getBooksLinksFromStorage())
    }

    fun saveBooksLinksToStorage() {
        booksLinks.filter { booksHashMap.contains(it.fileName) }
            .map { bookLink -> storageManager.updateProgressData(bookLink) }
        booksHashMap.clear()
    }

    fun upsertBook(bookLink: BookLink) {
        val exist = booksLinks.find { it.fileName == bookLink.fileName }
        if (exist != null) {
            if (exist == bookLink) return;
            booksLinks.remove(exist)
        }
        booksLinks.add(bookLink)
        booksHashMap.add(bookLink.fileName)
    }
}
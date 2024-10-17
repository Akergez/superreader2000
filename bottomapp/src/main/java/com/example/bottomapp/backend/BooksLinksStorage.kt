package com.example.bottomapp.backend

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class BookLink(val title: String, val filePath: String, val progress: Progress)

class BooksLinksStorage(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("BooksLinksStorage", Context.MODE_PRIVATE)

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
        val json = sharedPreferences.getString("booksLinks", null)
        return if (json != null) {
            val type = object : TypeToken<List<BookLink>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveBooksLinksToStorage() {
        val json = gson.toJson(booksLinks)
        sharedPreferences.edit().putString("booksLinks", json).apply()
    }

    fun upsertBook(bookLink: BookLink) {
        booksLinks.removeAll { it.filePath == bookLink.filePath }
        booksLinks.add(bookLink)
        saveBooksLinksToStorage()
    }
}
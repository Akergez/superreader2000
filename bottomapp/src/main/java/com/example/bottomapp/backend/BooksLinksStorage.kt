package com.example.bottomapp.backend

import android.content.Context
import android.content.SharedPreferences
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
        .registerSubtype(Fb2Progress::class.java, "fb2")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("books_links", Context.MODE_PRIVATE)
    val booksLinks: SnapshotStateList<BookLink> = mutableStateListOf()
    val booksHashMap: HashSet<String> = HashSet()

    init {
        refreshBooksLinksFromCache()
    }

    private suspend fun getBooksLinksFromStorage(): List<BookLink> {
        return storageManager.listAllBooks()
    }

    private fun getBooksLinksFromCache(): List<BookLink> {
        val cachedBooksLinks = sharedPreferences.getString("books_links_cache", null)
        return if (cachedBooksLinks != null) {
            val type = object : TypeToken<List<BookLink>>() {}.type
            gson.fromJson(cachedBooksLinks, type)
        } else {
            emptyList()
        }
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        if (booksLinks.toSet() == list.toSet()) return
        booksLinks.clear()
        booksLinks.addAll(list)
        cacheBooksLinks()
    }

    private fun refreshBooksLinksFromCache() {
        booksLinks.clear()
        booksLinks.addAll(getBooksLinksFromCache())
    }

    fun saveBooksLinksToStorage() {
        booksLinks.filter { booksHashMap.contains(it.fileName) }
            .map { bookLink -> storageManager.updateProgressData(bookLink) }
        booksHashMap.clear()
        cacheBooksLinks()
    }

    private fun cacheBooksLinks() {
        val editor = sharedPreferences.edit()
        val booksLinksJson = gson.toJson(booksLinks)
        editor.putString("books_links_cache", booksLinksJson)
        editor.apply()
    }

    fun upsertBook(bookLink: BookLink) {
        val exist = booksLinks.find { it.fileName == bookLink.fileName }
        if (exist != null) {
            if (exist == bookLink) return
            booksLinks.remove(exist)
        }
        booksLinks.add(bookLink)
        booksHashMap.add(bookLink.fileName)
    }
}
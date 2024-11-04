package com.example.bottomapp.backend

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

data class BookMetaInfo(val fileName: String, val title: String, val progress: Progress)

data class BookStorageEntry(val fileName: String, val previewUri: Uri?, val bookMetaInfo: BookMetaInfo)

class BookStorage(val context: Context, private val storageManager: StorageManager) {

    private val progressAdapterFactory = RuntimeTypeAdapterFactory
        .of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")
        .registerSubtype(Fb2Progress::class.java, "fb2")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("books_links", Context.MODE_PRIVATE)
    val bookCache: SnapshotStateList<BookStorageEntry> = mutableStateListOf()
    val booksHashMap: HashSet<String> = HashSet()

    init {
        refreshBooksLinksFromCache()
    }

    private suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBooks()
    }

    private fun getBooksLinksFromCache(): List<BookStorageEntry> {
        val cachedBooksLinks = sharedPreferences.getString("books_links_cache", null)
        return if (cachedBooksLinks != null) {
            val type = object : TypeToken<List<BookMetaInfo>>() {}.type
            gson.fromJson(cachedBooksLinks, type)
        } else {
            emptyList()
        }
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        if (bookCache.toSet() == list.toSet()) return
        bookCache.clear()
        bookCache.addAll(list)
        cacheBooksLinks()
    }

    private fun refreshBooksLinksFromCache() {
        bookCache.clear()
        bookCache.addAll(getBooksLinksFromCache())
    }

    fun saveBooksLinksToStorage() {
        bookCache.filter { booksHashMap.contains(it.bookMetaInfo.fileName) }
            .map { bookLink -> storageManager.updateProgressData(bookLink.bookMetaInfo) }
        booksHashMap.clear()
        cacheBooksLinks()
    }

    private fun cacheBooksLinks() {
        val editor = sharedPreferences.edit()
        val booksLinksJson = gson.toJson(bookCache)
        editor.putString("books_links_cache", booksLinksJson)
        editor.apply()
    }

    fun addNewBook(fileUri: Uri, fileName: String, progress: Progress)
    {
       val storageEntry = storageManager.copyBookToStorage(
            fileUri,
            BookMetaInfo(fileName, fileName, progress),
            context.contentResolver
        )
        bookCache.add(storageEntry)
    }

    fun updateProgress(bookMetaInfo: BookMetaInfo) {
        val exist = bookCache.find { it.bookMetaInfo.fileName == bookMetaInfo.fileName }
        if (exist == null) {
            return;
        }

        bookCache.remove(exist);
        bookCache.add(BookStorageEntry(exist.fileName, exist.previewUri, bookMetaInfo))
        saveBooksLinksToStorage()
    }
}
package com.example.bottomapp.shared.backend.storage

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Android implementation of book repository using Room DAO
 */

@Entity
data class Book(
    @PrimaryKey val fileName: String,
    @ColumnInfo(name = "payload") val payload: String?,
)

@Dao
interface UserDao {
    @Query("SELECT * FROM book")
    suspend fun getAll(): List<Book>

    @Query("SELECT * FROM book")
    fun getFlow(): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg users: Book)

    @Query("DELETE FROM book")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg users: Book)

    @Delete
    suspend fun delete(book: Book)
}

@Database(entities = [Book::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

actual class BookRepository(private val dao: UserDao, private val gson: Gson) : IBookRepository {
    private val _bookFlow = MutableStateFlow(emptyList<BookStorageEntry>())
    val bookFlow: StateFlow<List<BookStorageEntry>> = _bookFlow

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dao.getFlow().map {
                it.map {
                    gson.fromJson(it.payload, BookStorageEntry::class.java)
                }
            }.collect { value ->
                _bookFlow.value = value
            }
        }
    }

    actual override suspend fun saveBooks(books: List<BookStorageEntry>) {
        Log.i("BookStorageRepository", "saveBookLinks: $books")
        dao.deleteAll()
        dao.insertAll(*books.map { Book(it.bundleName, gson.toJson(it)) }.toTypedArray())
    }

    actual override suspend fun addBook(book: BookStorageEntry) {
        Log.i("BookStorageRepository", "addBook: $book")
        dao.insert(Book(book.bundleName, gson.toJson(book)))
    }

    actual override suspend fun updateBook(book: BookStorageEntry) {
        Log.i("BookStorageRepository", "updateBook: $book")
        dao.insert(Book(book.bundleName, gson.toJson(book)))
    }

    actual override suspend fun findBook(bundleName: String): BookStorageEntry? {
        Log.i("BookStorageRepository", "findBook: $bundleName")
        return dao.getAll().map { gson.fromJson(it.payload, BookStorageEntry::class.java) }
            .find { it.bundleName == bundleName }
    }
}

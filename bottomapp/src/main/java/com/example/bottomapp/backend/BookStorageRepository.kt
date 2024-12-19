package com.example.bottomapp.backend

import android.net.Uri
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
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

class BookStorageRepository(private val dao: UserDao) {
    private val progressAdapterFactory = RuntimeTypeAdapterFactory
        .of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")
        .registerSubtype(Fb2Progress::class.java, "fb2")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

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

    suspend fun saveBookLinks(bookCache: List<BookStorageEntry>) {
        dao.deleteAll()
        dao.insertAll(*bookCache.map { Book(it.bundleName, gson.toJson(it)) }.toTypedArray())
    }

    suspend fun addBook(book: BookStorageEntry) {
        dao.insert(Book(book.bundleName, gson.toJson(book)))
    }

    suspend fun updateBook(book: BookStorageEntry) {
        dao.insert(Book(book.bundleName, gson.toJson(book)))
    }

    suspend fun findBook(bundleName: String): BookStorageEntry? {
        return dao.getAll().map { gson.fromJson(it.payload, BookStorageEntry::class.java) }
            .find { it.bundleName == bundleName }
    }
}

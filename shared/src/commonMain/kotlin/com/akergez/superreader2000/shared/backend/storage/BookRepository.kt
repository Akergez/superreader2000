package com.akergez.superreader2000.shared.backend.storage

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BookRepository(private val dao: UserDao, private val json: Json) : IBookRepository {
    private val _bookFlow = MutableStateFlow(emptyList<BookStorageEntry>())
    val bookFlow: StateFlow<List<BookStorageEntry>> = _bookFlow

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dao.getFlow().map {
                it.map {
                    json.decodeFromString<BookStorageEntry>(it.payload ?: "{}")
                }
            }.collect { value ->
                _bookFlow.value = value
            }
        }
    }

    override suspend fun saveBooks(books: List<BookStorageEntry>) {
        //Log.i("BookStorageRepository", "saveBookLinks: $books")
        dao.deleteAll()
        dao.insertAll(*books.map { Book(it.bundleName, json.encodeToString(it)) }.toTypedArray())
    }

    override suspend fun addBook(book: BookStorageEntry) {
        //Log.i("BookStorageRepository", "addBook: $book")
        dao.insert(Book(book.bundleName, json.encodeToString(book)))
    }

    override suspend fun updateBook(book: BookStorageEntry) {
        // Log.i("BookStorageRepository", "updateBook: $book")
        dao.insert(Book(book.bundleName, json.encodeToString(book)))
    }

    override suspend fun findBook(bundleName: String): BookStorageEntry? {
        //Log.i("BookStorageRepository", "findBook: $bundleName")
        return dao.getAll().map { json.decodeFromString<BookStorageEntry>(it.payload ?: "{}") }
            .find { it.bundleName == bundleName }
    }
}

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


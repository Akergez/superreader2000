package com.example.bottomapp.backend

import android.net.Uri
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity
data class Book(
    @PrimaryKey val fileName: String,
    @ColumnInfo(name = "payload") val payload: String?,
)

@Dao
interface UserDao {
    @Query("SELECT * FROM book")
    suspend fun getAll(): List<Book>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg users: Book)
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

    suspend fun getBookLinks(): List<BookStorageEntry> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { gson.fromJson(it.payload, BookStorageEntry::class.java) }
        }
    }

    suspend fun saveBookLinks(bookCache: SnapshotStateList<BookStorageEntry>) {
        withContext(Dispatchers.IO) {
            dao.insertAll(*bookCache.map { Book(it.fileName, gson.toJson(it)) }.toTypedArray())
        }
    }
}

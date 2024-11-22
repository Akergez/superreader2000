package com.example.bottomapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.backend.AppDatabase
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.BookStorageRepository
import com.example.bottomapp.backend.StorageManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private val storageManager = inject<StorageManager>()
    private val bookStorage = inject<BookStorage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appModule = module {
            singleOf(::StorageManager) bind StorageManager::class
            singleOf(::BookStorage) bind BookStorage::class
            singleOf(::SettingsManager) bind SettingsManager::class
            singleOf(::BookStorageRepository) bind BookStorageRepository::class
            single {
                ImageLoader.Builder(androidContext())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache {
                        MemoryCache.Builder(androidContext()).maxSizePercent(0.25).build()
                    } // 25% of app's memory
                    .build()
            }
            single {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "database-name"
                )
                    .allowMainThreadQueries()
                    .build()
                db.userDao()
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@MainActivity)
            modules(appModule)
        }
        setContent {
            MainScreen()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        storageManager.value.clearFileCache()
        bookStorage.value.saveBooksLinksToStorage()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}
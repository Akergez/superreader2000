package com.example.bottomapp

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.backend.AppDatabase
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.BookStorageRepository
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.StorageManager
import com.example.bottomapp.ui.homescreen.HoomeScreenViewModel
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfScreenViewModel
import com.example.bottomapp.ui.settingsscreen.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

class ReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appModule = module {
            singleOf(::StorageManager) bind StorageManager::class
            singleOf(::BookStorage) bind BookStorage::class
            singleOf(::SettingsManager) bind SettingsManager::class
            singleOf(::BookStorageRepository) bind BookStorageRepository::class
            viewModel { SettingsViewModel(get(), get()) }
            viewModel { HoomeScreenViewModel(get()) }
            viewModel { parameters ->
                PdfScreenViewModel(
                    get(),
                    parameters.get(),
                    parameters.get()
                )
            }

            single {
                ImageLoader.Builder(androidContext())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache {
                        MemoryCache.Builder(androidContext()).maxSizePercent(0.25).build()
                    }
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
            androidContext(this@ReaderApp)
            modules(appModule)
        }
    }
}
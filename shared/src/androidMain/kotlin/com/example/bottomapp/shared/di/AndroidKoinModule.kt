package com.example.bottomapp.shared.di

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.example.bottomapp.shared.backend.settings.DefaultSettingsManager
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.AppDatabase
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.platform.FileUriHandler
import com.example.bottomapp.shared.platform.MessageHandler
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin module
 */
fun androidPlatformModule(context: Context): Module = module {
    single<MessageHandler> {
        MessageHandler(get<Context>())
    }

    single<FileUriHandler> {
        FileUriHandler(get<Context>())
    }

    single<BookRepository> {
        BookRepository(get(), get())
    }

    single<ISettingsManager> {
        DefaultSettingsManager(get())
    }

    single {
        val db = Room.databaseBuilder(
            context, AppDatabase::class.java, "database-name"
        ).allowMainThreadQueries().build()
        db.userDao()
    }
}


package com.akergez.superreader2000.shared.di

import android.content.Context
import androidx.room.Room
import com.akergez.superreader2000.shared.backend.settings.DefaultSettingsManager
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.storage.AppDatabase
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.platform.FileUriHandler
import com.akergez.superreader2000.shared.platform.MessageHandler
import org.koin.core.module.Module
import org.koin.dsl.module

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


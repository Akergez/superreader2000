package com.akergez.superreader2000.shared.di

import android.content.Context
import androidx.room.Room
import com.akergez.superreader2000.shared.backend.settings.DefaultSettingsManager
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.storage.AppDatabase
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.platform.FileUriHandler
import com.akergez.superreader2000.shared.platform.MessageHandler
import com.akergez.superreader2000.shared.backend.storage.FileService
import com.akergez.tresse.kmp.TresseVault
import com.akergez.tresse.kmp.safTresseVault
import com.akergez.tresse.kmp.TresseSync
import android.net.Uri
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single<FileService> { FileService(get()) }
    single<TresseVault> {
        val path = get<ISettingsManager>().getStoragePath() ?: error("Storage directory not set")
        safTresseVault(get(), Uri.parse(path))
    }
    single { TresseSync(get<TresseVault>()) }
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

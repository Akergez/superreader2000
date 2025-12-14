package com.example.bottomapp.shared.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.BookStorage
import com.example.bottomapp.shared.formats.abstractions.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.storage.AppDatabase
import com.example.bottomapp.shared.backend.storage.BookMetaInfoDeserializer
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.IDeserializer
import com.example.bottomapp.shared.backend.storage.INameProvider
import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.platform.FileUriHandler
import com.example.bottomapp.shared.platform.MessageHandler
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

fun desktopPlatformModule(): Module = module {
    single<MessageHandler> {
        MessageHandler()
    }

    single<FileUriHandler> {
        FileUriHandler()
    }

    single {
        val dbFile = File(System.getProperty("user.home"), ".local/.bottomapp/cache.db")
        dbFile.parentFile?.mkdirs()
        Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
            .userDao()
    }

    single<IDeserializer<BookMetaInfo>> { BookMetaInfoDeserializer(get()) }

    single<INameProvider<BookMetaInfo>> { BookMetaInfoDeserializer(get()) }

    single<BookStorage> {
        BookStorage(
            get(),
            get(),
            get(),
            get<BookInfoExtractorFactory>(),
            mapOf(
                FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                FormatType.FB2 to get(qualifier = named(FormatType.FB2))
            )
        )
    }

    single<BookRepository> {
        BookRepository(get(), get())
    }

    single<BookManager> {
        BookManager(get(), get(), get())
    }
}


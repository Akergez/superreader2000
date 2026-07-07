package com.akergez.superreader2000.shared.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.BookStorage
import com.akergez.superreader2000.shared.formats.abstractions.BookInfoExtractorFactory
import com.akergez.superreader2000.shared.backend.storage.AppDatabase
import com.akergez.superreader2000.shared.backend.storage.BookMetaInfoDeserializer
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.backend.storage.FileService
import com.akergez.superreader2000.shared.backend.storage.IDeserializer
import com.akergez.superreader2000.shared.backend.storage.INameProvider
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.djvu.DjvuPreviewGenerator
import com.akergez.superreader2000.shared.formats.fb2.Fb2PreviewGenerator
import com.akergez.superreader2000.shared.formats.pdf.PdfPreviewGenerator
import com.akergez.superreader2000.shared.platform.FileUriHandler
import com.akergez.superreader2000.shared.platform.MessageHandler
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.tresse.kmp.TresseVault
import com.akergez.tresse.kmp.fileTresseVault
import com.akergez.tresse.kmp.TresseSync

fun desktopPlatformModule(): Module = module {
    single<MessageHandler> {
        MessageHandler()
    }

    single<FileUriHandler> {
        FileUriHandler()
    }

    single<FileService> {
        FileService()
    }
    single<TresseVault> {
        val path = get<ISettingsManager>().getStoragePath() ?: error("Storage directory not set")
        fileTresseVault(File(path))
    }
    single { TresseSync(get<TresseVault>()) }

    single {
        val dbFile = File(System.getProperty("user.home"), ".local/.superreader2000/cache.db")
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

    single<IPreviewGenerator>(named(FormatType.PDF)) { PdfPreviewGenerator() }
    single<IPreviewGenerator>(named(FormatType.FB2)) { Fb2PreviewGenerator() }
    single<IPreviewGenerator>(named(FormatType.DJVU)) { DjvuPreviewGenerator() }

    single<BookStorage> {
        BookStorage(
            get(),
            get(),
            get(),
            get<BookInfoExtractorFactory>(),
            mapOf(
                FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                FormatType.FB2 to get(qualifier = named(FormatType.FB2)),
                FormatType.DJVU to get(qualifier = named(FormatType.DJVU))
            ),
            get()
        )
    }

    single<BookRepository> {
        BookRepository(get(), get())
    }

    single<BookManager> {
        BookManager(get(), get(), get(), get())
    }
}

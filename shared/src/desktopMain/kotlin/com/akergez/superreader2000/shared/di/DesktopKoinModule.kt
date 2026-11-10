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
import com.akergez.superreader2000.shared.sync.DesktopServerConnectHost
import com.akergez.superreader2000.shared.sync.ServerConnectHost
import com.akergez.tresse.kmp.TresseFile
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
        SettingsBackedFileTresseVault(get())
    }
    single { TresseSync(get<TresseVault>()) }
    single<ServerConnectHost> { DesktopServerConnectHost() }

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
        BookManager(get(), get(), get(), get(), get())
    }
}

internal class SettingsBackedFileTresseVault(
    private val settings: ISettingsManager,
) : TresseVault {
    @Volatile private var cachedPath: String? = null
    @Volatile private var cachedVault: TresseVault? = null

    private fun delegate(): TresseVault {
        val path = settings.getStoragePath() ?: error("Storage directory not set")
        cachedVault?.takeIf { cachedPath == path }?.let { return it }
        return synchronized(this) {
            cachedVault?.takeIf { cachedPath == path } ?: fileTresseVault(File(path)).also {
                cachedPath = path
                cachedVault = it
            }
        }
    }

    override fun listFiles(): List<TresseFile> = delegate().listFiles()
    override fun read(path: String, offset: Long, length: Int): ByteArray = delegate().read(path, offset, length)
    override fun writeText(path: String, content: String) = delegate().writeText(path, content)
    override fun writeBinary(path: String, bytes: ByteArray) = delegate().writeBinary(path, bytes)
    override fun delete(path: String) = delegate().delete(path)
    override fun storeList(directory: String): List<String> = delegate().storeList(directory)
    override fun storeRead(path: String): ByteArray? = delegate().storeRead(path)
    override fun storeWrite(path: String, bytes: ByteArray) = delegate().storeWrite(path, bytes)
    override fun storeDelete(path: String) = delegate().storeDelete(path)
}

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
import com.akergez.superreader2000.shared.sync.AndroidServerConnectHost
import com.akergez.superreader2000.shared.sync.ServerConnectHost
import com.akergez.tresse.kmp.TresseFile
import com.akergez.tresse.kmp.TresseVault
import com.akergez.tresse.kmp.safTresseVault
import com.akergez.tresse.kmp.TresseSync
import android.net.Uri
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single<FileService> { FileService(get()) }
    single<TresseVault> {
        SettingsBackedAndroidTresseVault(get(), get())
    }
    single { TresseSync(get<TresseVault>()) }
    single<ServerConnectHost> { AndroidServerConnectHost(get()) }
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

private class SettingsBackedAndroidTresseVault(
    private val context: Context,
    private val settings: ISettingsManager,
) : TresseVault {
    @Volatile private var cachedPath: String? = null
    @Volatile private var cachedVault: TresseVault? = null

    private fun delegate(): TresseVault {
        val path = settings.getStoragePath() ?: error("Storage directory not set")
        cachedVault?.takeIf { cachedPath == path }?.let { return it }
        return synchronized(this) {
            cachedVault?.takeIf { cachedPath == path } ?: safTresseVault(context, Uri.parse(path)).also {
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

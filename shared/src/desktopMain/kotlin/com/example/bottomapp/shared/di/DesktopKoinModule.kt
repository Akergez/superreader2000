package com.example.bottomapp.shared.di

import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.platform.FileUriHandler
import com.example.bottomapp.shared.platform.MessageHandler
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Desktop-specific Koin module
 */
fun desktopPlatformModule(): Module = module {
    single<MessageHandler> {
        MessageHandler()
    }

    single<FileUriHandler> {
        FileUriHandler()
    }

    single<BookRepository> {
        BookRepository(get<PreferencesStore>())
    }

    singleOf(::BookManager) bind BookManager::class
}


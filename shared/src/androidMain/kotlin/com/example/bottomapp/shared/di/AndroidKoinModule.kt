package com.example.bottomapp.shared.di

import android.content.Context
import com.example.bottomapp.shared.platform.FileUriHandler
import com.example.bottomapp.shared.platform.MessageHandler
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin module
 */
fun androidPlatformModule(): Module = module {
    single<MessageHandler> {
        MessageHandler(get<Context>())
    }

    single<FileUriHandler> {
        FileUriHandler(get<Context>())
    }
}


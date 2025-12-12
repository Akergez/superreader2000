package com.example.bottomapp.shared.di

import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.bookCreators.Fb2CreateFactory
import com.example.bottomapp.shared.backend.bookCreators.IBookCreateFactory
import com.example.bottomapp.shared.backend.bookCreators.PdfCreateFactory
import com.example.bottomapp.shared.backend.bookExtractors.BookExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.DjvuBookAuthorExtractor
import com.example.bottomapp.shared.backend.bookExtractors.Fb2BookAuthorExtractor
import com.example.bottomapp.shared.backend.bookExtractors.IBookInfoExtractor
import com.example.bottomapp.shared.backend.bookExtractors.IBookInfoExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.PdfBookAuthorExtractor
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FileType
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.PdfPreviewGenerator
import com.example.bottomapp.shared.backend.settings.DefaultSettingsManager
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.platform.FileUriHandler
import com.example.bottomapp.shared.platform.MessageHandler
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Common Koin module for shared business logic
 * Platform-specific modules should add to this
 */
fun commonModule(): Module = module {
    // JSON serialization configuration
    single<Json> {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    // Book extractors factory

    single {
        DefaultSettingsManager(get<PreferencesStore>())
    } bind ISettingsManager::class

    // Shared module - Book Repository

    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { PdfBookAuthorExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { Fb2BookAuthorExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { DjvuBookAuthorExtractor() }

    single {
        BookInfoExtractorFactory(
            mapOf(
                FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                FormatType.FB2 to get(qualifier = named(FormatType.FB2))
            )
        )
    }

    single<IBookCreateFactory>(named(FileType.pdf)) { PdfCreateFactory() }
    single<IBookCreateFactory>(named(FileType.fb2)) { Fb2CreateFactory() }


    // Book extractors
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { BookExtractorFactory().createPdfExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { BookExtractorFactory().createDjvuExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { BookExtractorFactory().createFb2Extractor() }
    singleOf(::BookRepository)
}


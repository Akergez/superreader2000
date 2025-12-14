package com.example.bottomapp.shared.di

import com.example.bottomapp.shared.formats.fb2.Fb2CreateFactory
import com.example.bottomapp.shared.formats.abstractions.IBookCreateFactory
import com.example.bottomapp.shared.formats.pdf.PdfCreateFactory
import com.example.bottomapp.shared.formats.abstractions.BookExtractorFactory
import com.example.bottomapp.shared.formats.abstractions.BookInfoExtractorFactory
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractor
import com.example.bottomapp.shared.backend.settings.DefaultSettingsManager
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.FileType
import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.Progress
import com.example.bottomapp.shared.formats.djvu.DjvuBookAuthorExtractor
import com.example.bottomapp.shared.formats.djvu.DjvuProgress
import com.example.bottomapp.shared.formats.fb2.Fb2BookAuthorExtractor
import com.example.bottomapp.shared.formats.fb2.Fb2Progress
import com.example.bottomapp.shared.formats.pdf.PdfBookAuthorExtractor
import com.example.bottomapp.shared.formats.pdf.PdfProgress
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

fun commonModule(): Module = module {
    single {
        DefaultSettingsManager(get<PreferencesStore>())
    } bind ISettingsManager::class

    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { PdfBookAuthorExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { Fb2BookAuthorExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { DjvuBookAuthorExtractor() }

    //ToDo extract
    val module = SerializersModule {
        polymorphic(Progress::class) {
            subclass(PdfProgress::class)
            subclass(Fb2Progress::class)
            subclass(DjvuProgress::class)
        }
    }

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        serializersModule = module
    }

    single<Json> { json }

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

    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { BookExtractorFactory().createPdfExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { BookExtractorFactory().createDjvuExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { BookExtractorFactory().createFb2Extractor() }
    singleOf(::BookRepository)
}


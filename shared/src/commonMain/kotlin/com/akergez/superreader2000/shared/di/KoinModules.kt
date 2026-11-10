package com.akergez.superreader2000.shared.di

import com.akergez.superreader2000.shared.formats.fb2.Fb2CreateFactory
import com.akergez.superreader2000.shared.formats.abstractions.IBookCreateFactory
import com.akergez.superreader2000.shared.formats.pdf.PdfCreateFactory
import com.akergez.superreader2000.shared.formats.abstractions.BookExtractorFactory
import com.akergez.superreader2000.shared.formats.abstractions.BookInfoExtractorFactory
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import com.akergez.superreader2000.shared.backend.settings.DefaultSettingsManager
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.backend.storage.PreferencesStore
import com.akergez.superreader2000.shared.sync.ServerConnectStore
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.FileType
import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import com.akergez.superreader2000.shared.formats.djvu.DjvuBookAuthorExtractor
import com.akergez.superreader2000.shared.formats.djvu.DjvuCreateFactory
import com.akergez.superreader2000.shared.formats.djvu.DjvuProgress
import com.akergez.superreader2000.shared.formats.fb2.Fb2BookAuthorExtractor
import com.akergez.superreader2000.shared.formats.fb2.Fb2Progress
import com.akergez.superreader2000.shared.formats.pdf.PdfBookAuthorExtractor
import com.akergez.superreader2000.shared.formats.pdf.PdfProgress
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
    single { ServerConnectStore(get(), get()) }
    single {
        BookInfoExtractorFactory(
            mapOf(
                FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                FormatType.FB2 to get(qualifier = named(FormatType.FB2)),
                FormatType.DJVU to get(qualifier = named(FormatType.DJVU))
            )
        )
    }

    single<IBookCreateFactory>(named(FileType.pdf)) { PdfCreateFactory() }
    single<IBookCreateFactory>(named(FileType.fb2)) { Fb2CreateFactory() }
    single<IBookCreateFactory>(named(FileType.djvu)) { DjvuCreateFactory() }

    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { BookExtractorFactory().createPdfExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { BookExtractorFactory().createDjvuExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { BookExtractorFactory().createFb2Extractor() }
    singleOf(::BookRepository)
}

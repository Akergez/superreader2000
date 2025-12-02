package com.example.bottomapp

import android.app.Application
import android.net.Uri
import androidx.room.Room
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.backend.AppDatabase
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.BookMetaInfo
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.BookStorageRepository
import com.example.bottomapp.backend.DjvuProgress
import com.example.bottomapp.backend.Fb2BookAuthorExtractor
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.backend.IBookInfoExtractor
import com.example.bottomapp.backend.PdfBookAuthorExtractor
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.Progress
import com.example.bottomapp.backend.RuntimeTypeAdapterFactory
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.StorageAdapter
import com.example.bottomapp.backend.UriTypeAdapter
import com.example.bottomapp.backend.bookCreators.Fb2CreateFactory
import com.example.bottomapp.backend.bookCreators.IBookCreateFactory
import com.example.bottomapp.backend.bookCreators.PdfCreateFactory
import com.example.bottomapp.backend.previewGenerators.Fb2PreviewGenerator
import com.example.bottomapp.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.backend.previewGenerators.PdfPreviewGenerator
import com.example.bottomapp.constants.FileType
import com.example.bottomapp.ui.homescreen.HoomeScreenViewModel
import com.example.bottomapp.ui.homescreen.components.EditModalViewModel
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.Fb2ReaderScreenProvider
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.IReaderScreenProvider
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.PdfReaderScreenProvider
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfScreenViewModel
import com.example.bottomapp.ui.settingsscreen.SettingsViewModel
import com.google.gson.GsonBuilder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

class ReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appModule = module {
            single<IBookCreateFactory>(named(FileType.pdf)) { PdfCreateFactory() }
            single<IBookCreateFactory>(named(FileType.fb2)) { Fb2CreateFactory() }
            // DjVu disabled - requires native library

            single<IReaderScreenProvider>(named(FormatType.PDF)) { PdfReaderScreenProvider(get()) }
            single<IReaderScreenProvider>(named(FormatType.FB2)) { Fb2ReaderScreenProvider(get()) }
            // DjVu disabled

            single<IPreviewGenerator>(named(FormatType.PDF)) { PdfPreviewGenerator() }
            single<IPreviewGenerator>(named(FormatType.FB2)) { Fb2PreviewGenerator() }
            // DjVu disabled

            single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { PdfBookAuthorExtractor() }
            single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { Fb2BookAuthorExtractor() }
            single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { Fb2BookAuthorExtractor() }


            single {
                GsonBuilder().registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory.of(Progress::class.java, "type")
                        .registerSubtype(PdfProgress::class.java, "pdf")
                        .registerSubtype(Fb2Progress::class.java, "fb2")
                        .registerSubtype(DjvuProgress::class.java, "djvu")
                ).registerTypeAdapter(Uri::class.java, UriTypeAdapter()).create()
            }

            singleOf(::StorageAdapter) bind StorageAdapter::class
            single {
                BookStorage(
                    androidContext(), get(), get(), get(), mapOf(
                        FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                        FormatType.FB2 to get(qualifier = named(FormatType.FB2))
                    )
                )
            }
            singleOf(::BookManager) bind BookManager::class
            singleOf(::SettingsManager) bind SettingsManager::class
            singleOf(::BookStorageRepository) bind BookStorageRepository::class
            single {
                BookInfoExtractorFactory(
                    mapOf(
                        FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                        FormatType.FB2 to get(qualifier = named(FormatType.FB2))
                    )
                )
            }
            viewModel { parameters -> EditModalViewModel(parameters.get()) }
            viewModel { SettingsViewModel(get(), get()) }
            viewModel {
                HoomeScreenViewModel(
                    get(), mapOf(
                        FileType.pdf to get(qualifier = named(FormatType.PDF)),
                        FileType.fb2 to get(qualifier = named(FormatType.FB2))
                    )
                )
            }
            viewModel { parameters ->
                PdfScreenViewModel(
                    get(), parameters.get(), parameters.get()
                )
            }
            // DjVu screen disabled

            single {
                ImageLoader.Builder(androidContext()).memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache {
                        MemoryCache.Builder(androidContext()).maxSizePercent(0.25).build()
                    }.build()
            }
            single {
                val db = Room.databaseBuilder(
                    applicationContext, AppDatabase::class.java, "database-name"
                ).allowMainThreadQueries().build()
                db.userDao()
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@ReaderApp)
            modules(appModule)
        }
        PDFBoxResourceLoader.init(applicationContext)
    }
}
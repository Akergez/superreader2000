package com.example.bottomapp

import android.app.Application
import android.net.Uri
import androidx.room.Room
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.backend.AppDatabase
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.Fb2BookAuthorExtractor
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.IBookInfoExtractor
import com.example.bottomapp.backend.PdfBookAuthorExtractor
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.Progress
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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
// Shared module imports
import com.example.bottomapp.shared.backend.domain.FormatType as SharedFormatType
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.settings.DefaultSettingsManager

class ReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appModule = module {
            // Shared module - Settings and Preferences
            single<PreferencesStore> {
                PreferencesStore(androidContext())
            }
            single {
                DefaultSettingsManager(get<PreferencesStore>())
            }

            // Shared module - Book Repository
            single<BookRepository> {
                BookRepository(get<PreferencesStore>())
            }

            // Android-specific UI providers
            single<IBookCreateFactory>(named(FileType.pdf)) { PdfCreateFactory() }
            single<IBookCreateFactory>(named(FileType.fb2)) { Fb2CreateFactory() }

            single<IReaderScreenProvider>(named(SharedFormatType.PDF)) { PdfReaderScreenProvider(get()) }
            single<IReaderScreenProvider>(named(SharedFormatType.FB2)) { Fb2ReaderScreenProvider(get()) }

            single<IPreviewGenerator>(named(SharedFormatType.PDF)) { PdfPreviewGenerator() }
            single<IPreviewGenerator>(named(SharedFormatType.FB2)) { Fb2PreviewGenerator() }

            // Book info extractors (using local Android types for now)
            single<IBookInfoExtractor<com.example.bottomapp.backend.BookMetaInfo>>(named(SharedFormatType.PDF)) {
                PdfBookAuthorExtractor()
            }
            single<IBookInfoExtractor<com.example.bottomapp.backend.BookMetaInfo>>(named(SharedFormatType.FB2)) {
                Fb2BookAuthorExtractor()
            }

            // Android-specific storage (legacy, kept for backward compatibility)
            singleOf(::StorageAdapter) bind StorageAdapter::class
            singleOf(::BookManager) bind BookManager::class

            // Image loader for Coil
            single {
                ImageLoader.Builder(androidContext()).memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache {
                        MemoryCache.Builder(androidContext()).maxSizePercent(0.25).build()
                    }.build()
            }

            // Android Room database (kept for now, will migrate to SQLDelight in Phase 3)
            single {
                val db = Room.databaseBuilder(
                    androidContext(), AppDatabase::class.java, "database-name"
                ).allowMainThreadQueries().build()
                db.userDao()
            }

            // ViewModels
            viewModel { parameters -> EditModalViewModel(parameters.get()) }
            viewModel {
                SettingsViewModel(get(), get())
            }
            viewModel {
                HoomeScreenViewModel(
                    get(), mapOf(
                        FileType.pdf to get(qualifier = named(SharedFormatType.PDF)),
                        FileType.fb2 to get(qualifier = named(SharedFormatType.FB2))
                    )
                )
            }
            viewModel { parameters ->
                PdfScreenViewModel(
                    get(), parameters.get(), parameters.get()
                )
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
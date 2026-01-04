package com.example.bottomapp

import android.app.Application
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.BookStorage
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.storage.BookMetaInfoDeserializer
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.FileService
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.di.androidPlatformModule
import com.example.bottomapp.shared.di.commonModule
import com.example.bottomapp.shared.formats.abstractions.FileType
import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.IReaderScreenProvider
import com.example.bottomapp.shared.formats.fb2.Fb2PreviewGenerator
import com.example.bottomapp.shared.formats.fb2.ui.Fb2ReaderScreenProvider
import com.example.bottomapp.shared.formats.pdf.PdfPreviewGenerator
import com.example.bottomapp.shared.formats.pdf.ui.PdfReaderScreenProvider
import com.example.bottomapp.shared.formats.pdf.ui.state.PdfScreenViewModel
import com.example.bottomapp.shared.ui.components.EditModalViewModel
import com.example.bottomapp.shared.ui.screens.homescreen.HoomeScreenViewModel
import com.example.bottomapp.shared.ui.screens.settingsscreen.SettingsViewModel
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
            single<PreferencesStore> {
                PreferencesStore(androidContext())
            }

            single<FileService> {
                FileService(androidContext())
            }

            single<IReaderScreenProvider>(named(FormatType.PDF)) { PdfReaderScreenProvider(get(), get()) }
            single<IReaderScreenProvider>(named(FormatType.FB2)) { Fb2ReaderScreenProvider(get()) }

            single<IPreviewGenerator>(named(FormatType.PDF)) { PdfPreviewGenerator() }
            single<IPreviewGenerator>(named(FormatType.FB2)) { Fb2PreviewGenerator() }

            singleOf(::BookMetaInfoDeserializer) bind BookMetaInfoDeserializer::class
            singleOf(::BookManager) bind BookManager::class

            single {
                ImageLoader.Builder(androidContext()).memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache {
                        MemoryCache.Builder(androidContext()).maxSizePercent(0.25).build()
                    }.build()
            }

            single {
                BookStorage(
                    get(), get(), get(), mapOf(
                        FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                        FormatType.FB2 to get(qualifier = named(FormatType.FB2))
                    ),
                    get()
                )
            }

            singleOf(::BookManager) bind BookManager::class
            singleOf(::BookRepository) bind BookRepository::class

            viewModel { parameters -> EditModalViewModel(parameters.get()) }
            viewModel {
                SettingsViewModel(get(), get())
            }
            viewModel {
                HoomeScreenViewModel(
                    get(),
                    mapOf(
                        FileType.pdf to get(qualifier = named(FormatType.PDF)),
                        FileType.fb2 to get(qualifier = named(FormatType.FB2))
                    ),
                    get(),
                    get()
                )
            }
            viewModel { parameters ->
                PdfScreenViewModel(
                    parameters.get(), parameters.get(), parameters.get()
                )
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@ReaderApp)
            modules(commonModule(), androidPlatformModule(applicationContext), appModule)
        }
        PDFBoxResourceLoader.init(applicationContext)
    }
}
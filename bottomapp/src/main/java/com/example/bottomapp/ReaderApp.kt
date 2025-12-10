package com.example.bottomapp

// Shared module imports
import android.app.Application
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.BookStorage
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.shared.backend.StorageAdapter
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.Fb2PreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.PdfPreviewGenerator
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.constants.FileType
import com.example.bottomapp.shared.di.commonModule
import com.example.bottomapp.shared.ui.screens.readerscreens.pdfscreen.PdfScreenViewModel
import com.example.bottomapp.ui.homescreen.HoomeScreenViewModel
import com.example.bottomapp.ui.homescreen.components.EditModalViewModel
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.Fb2ReaderScreenProvider
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.IReaderScreenProvider
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.PdfReaderScreenProvider
import com.example.bottomapp.shared.ui.screens.settingsscreen.SettingsViewModel
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
import com.example.bottomapp.shared.backend.domain.FormatType as SharedFormatType

class ReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appModule = module {
            // Shared module - Settings and Preferences
            single<PreferencesStore> {
                PreferencesStore(androidContext())
            }

            // Android-specific UI providers

            single<IReaderScreenProvider>(named(SharedFormatType.PDF)) { PdfReaderScreenProvider(get()) }
            single<IReaderScreenProvider>(named(SharedFormatType.FB2)) { Fb2ReaderScreenProvider(get()) }

            single<IPreviewGenerator>(named(SharedFormatType.PDF)) { PdfPreviewGenerator() }
            single<IPreviewGenerator>(named(SharedFormatType.FB2)) { Fb2PreviewGenerator() }

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

            single {
                GsonBuilder().create()
            }

            /*.registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory.of(Progress, "type")
                        .registerSubtype(PdfProgress::class.java, "pdf")
                        .registerSubtype(Fb2Progress::class.java, "fb2")
                        .registerSubtype(DjvuProgress::class.java, "djvu")
                ).registerTypeAdapter(Uri::class.java, UriTypeAdapter())*/


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
            // ViewModels
            singleOf(::BookRepository) bind BookRepository::class

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
                    parameters.get(), parameters.get(), parameters.get()
                )
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@ReaderApp)
            modules(commonModule(), appModule)
        }
        PDFBoxResourceLoader.init(applicationContext)
    }
}
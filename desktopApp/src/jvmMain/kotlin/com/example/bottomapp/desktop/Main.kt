package com.example.bottomapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.Fb2PreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.PdfPreviewGenerator
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.constants.FileType
import com.example.bottomapp.shared.di.commonModule
import com.example.bottomapp.shared.di.desktopPlatformModule
import com.example.bottomapp.shared.ui.components.EditModalViewModel
import com.example.bottomapp.shared.ui.screens.homescreen.HoomeScreenViewModel
import com.example.bottomapp.shared.ui.screens.mainscreen.MainScreen
import com.example.bottomapp.shared.ui.screens.mainscreen.readerscreenproviders.Fb2ReaderScreenProvider
import com.example.bottomapp.shared.ui.screens.mainscreen.readerscreenproviders.IReaderScreenProvider
import com.example.bottomapp.shared.ui.screens.mainscreen.readerscreenproviders.PdfReaderScreenProvider
import com.example.bottomapp.shared.ui.screens.readerscreens.pdfscreen.PdfScreenViewModel
import com.example.bottomapp.shared.ui.screens.settingsscreen.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module


fun main() {
    // Set macOS appearance to respect system theme (dark mode)
    // This must be set BEFORE the application starts
    System.setProperty("apple.awt.application.appearance", "system")

    application {
        // Initialize Koin before creating the UI
        startKoin {
            modules(
                commonModule(), desktopPlatformModule(), desktopAppModule()
            )
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "PDF Reader KMP"
        ) {
            MainScreen()
        }
    }
}

/**
 * Desktop-specific application module
 */
private fun desktopAppModule() = module {
    // Preferences Store
    single<PreferencesStore> {
        PreferencesStore()
    }

    // Reader Screen Providers
    single<IReaderScreenProvider>(named(FormatType.PDF)) {
        PdfReaderScreenProvider(get(), get())
    }
    single<IReaderScreenProvider>(named(FormatType.FB2)) {
        Fb2ReaderScreenProvider(get())
    }

    // Preview Generators
    single<IPreviewGenerator>(named(FormatType.PDF)) {
        PdfPreviewGenerator()
    }
    single<IPreviewGenerator>(named(FormatType.FB2)) {
        Fb2PreviewGenerator()
    }

    // ViewModels
    viewModel {
        SettingsViewModel(get(), get())
    }

    viewModel { p -> EditModalViewModel(p.get()) }

    viewModel {
        HoomeScreenViewModel(
            get(), mapOf(
                FileType.pdf to get(qualifier = named(FormatType.PDF)),
                FileType.fb2 to get(qualifier = named(FormatType.FB2))
            ), get(), get()
        )
    }

    viewModel { parameters ->
        PdfScreenViewModel(
            parameters.get(), parameters.get(), parameters.get()
        )
    }
}



package com.example.bottomapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.bottomapp.shared.formats.fb2.Fb2PreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.formats.pdf.PdfPreviewGenerator
import com.example.bottomapp.shared.backend.storage.PreferencesStore
import com.example.bottomapp.shared.di.commonModule
import com.example.bottomapp.shared.di.desktopPlatformModule
import com.example.bottomapp.shared.formats.abstractions.FileType
import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.ui.components.EditModalViewModel
import com.example.bottomapp.shared.ui.screens.homescreen.HoomeScreenViewModel
import com.example.bottomapp.shared.ui.screens.mainscreen.MainScreen
import com.example.bottomapp.shared.formats.fb2.ui.Fb2ReaderScreenProvider
import com.example.bottomapp.shared.formats.abstractions.IReaderScreenProvider
import com.example.bottomapp.shared.formats.pdf.ui.PdfReaderScreenProvider
import com.example.bottomapp.shared.formats.pdf.ui.state.PdfScreenViewModel
import com.example.bottomapp.shared.ui.screens.settingsscreen.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module


fun main() {
    System.setProperty("apple.awt.application.appearance", "system")

    application {
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



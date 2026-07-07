package com.akergez.superreader2000.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.akergez.superreader2000.shared.formats.fb2.Fb2PreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.formats.pdf.PdfPreviewGenerator
import com.akergez.superreader2000.shared.backend.storage.PreferencesStore
import com.akergez.superreader2000.shared.di.commonModule
import com.akergez.superreader2000.shared.di.desktopPlatformModule
import com.akergez.superreader2000.shared.formats.abstractions.FileType
import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.ui.components.EditModalViewModel
import com.akergez.superreader2000.shared.ui.screens.homescreen.HoomeScreenViewModel
import com.akergez.superreader2000.shared.ui.screens.mainscreen.MainScreen
import com.akergez.superreader2000.shared.formats.fb2.ui.Fb2ReaderScreenProvider
import com.akergez.superreader2000.shared.formats.abstractions.IReaderScreenProvider
import com.akergez.superreader2000.shared.formats.pdf.ui.PdfReaderScreenProvider
import com.akergez.superreader2000.shared.formats.pdf.ui.state.PdfScreenViewModel
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.SettingsViewModel
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
        SettingsViewModel(get(), get(), get())
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


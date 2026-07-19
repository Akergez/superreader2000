package com.akergez.superreader2000.shared.formats.fb2.ui

import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider
import com.akergez.superreader2000.shared.formats.fb2.ui.components.Fb2Viewer
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode

class Fb2ViewerProvider(
    private val documentTheme: DocThemeMode,
    private val compatibilityTextRenderer: Boolean,
) : IViewerProvider<Fb2BookContent> {
    override fun getViewer(
        bookContent: Fb2BookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {

        return {
            Fb2Viewer(
                content = bookContent,
                documentTheme = documentTheme,
                compatibilityTextRenderer = compatibilityTextRenderer,
            )
        }

    }
}

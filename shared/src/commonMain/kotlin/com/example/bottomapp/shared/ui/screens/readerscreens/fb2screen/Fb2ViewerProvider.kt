package com.example.bottomapp.shared.ui.screens.readerscreens.fb2screen

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.ui.Fb2BookContent
import com.example.bottomapp.shared.ui.screens.readerscreens.common.IViewerProvider

class Fb2ViewerProvider : IViewerProvider<Fb2BookContent> {
    override fun getViewer(
        bookContent: Fb2BookContent,
        applyInversion: Boolean
    ): @Composable () -> Unit {

        return {
            Fb2Viewer(
                content = bookContent,
                applyInversion = applyInversion,
                offsetY = 0f,
                offsetX = 0f,
                scale = 1f
            )
        }

    }
}
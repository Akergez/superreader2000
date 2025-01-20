package com.example.bottomapp.ui.readerscreens.fb2screen

import androidx.compose.runtime.Composable
import com.example.bottomapp.ui.readerscreens.Fb2BookContent
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.IViewerProvider

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
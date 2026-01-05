package com.akergez.superreader2000.shared.ui.screens.readerscreens.common

import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.ui.IBookContent

interface IViewerProvider<T> where T : IBookContent{
    fun getViewer(bookContent: T, applyInversion: Boolean): @Composable () -> Unit
}
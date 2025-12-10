package com.example.bottomapp.shared.ui.screens.readerscreens.common

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.ui.IBookContent

interface IViewerProvider<T> where T : IBookContent{
    fun getViewer(bookContent: T, applyInversion: Boolean): @Composable () -> Unit
}
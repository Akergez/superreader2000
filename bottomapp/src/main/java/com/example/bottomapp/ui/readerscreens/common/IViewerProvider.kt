package com.example.bottomapp.ui.readerscreens.common

import androidx.compose.runtime.Composable
import com.example.bottomapp.ui.readerscreens.IBookContent

interface IViewerProvider<T> where T : IBookContent{
    fun getViewer(bookContent: T, applyInversion: Boolean): @Composable () -> Unit
}
package com.example.bottomapp.backend.renderHelper

import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.ui.readerscreens.IBookContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IRenderHelper {
    fun openPage(pageIndex: Int): StateFlow<IBookContent>
    fun close()
    fun getBookType(): FormatType
}
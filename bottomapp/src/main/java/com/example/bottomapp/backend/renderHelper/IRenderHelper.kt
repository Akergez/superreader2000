package com.example.bottomapp.backend.renderHelper

import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.ui.readerscreens.IBookContent
import kotlinx.coroutines.flow.MutableStateFlow

interface IRenderHelper {
    fun openPage(pageIndex: Int): MutableStateFlow<IBookContent>
    fun openPagedText(pageIndex: Int): MutableStateFlow<IBookContent>
    fun close()
    fun getBookType(): FormatType
}
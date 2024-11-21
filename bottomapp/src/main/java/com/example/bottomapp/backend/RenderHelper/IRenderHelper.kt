package com.example.bottomapp.backend.RenderHelper

import android.graphics.Bitmap
import com.example.bottomapp.backend.FormatType

interface IRenderHelper {
    fun getPageCount(): Int
    fun openPage(pageIndex: Int): Bitmap
    fun openPagedText(pageIndex: Int): String
    fun close()
    fun getBookType(): FormatType
}
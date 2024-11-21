package com.example.bottomapp.backend.RenderHelper

import android.graphics.Bitmap

interface IRenderHelper {
    fun getPageCount(): Int
    fun openPage(pageIndex: Int): Bitmap
    fun close()
}
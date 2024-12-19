package com.example.bottomapp.ui.readerscreens

import android.graphics.Bitmap

interface IBookContent

data class PdfBookContent(val bitmap: Bitmap?): IBookContent

data class Fb2BookContent(val text: String?): IBookContent
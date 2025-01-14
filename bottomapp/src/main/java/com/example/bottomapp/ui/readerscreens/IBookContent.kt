package com.example.bottomapp.ui.readerscreens

import android.graphics.Bitmap
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.AnnotatedString

interface IBookContent

data class PdfBookContent(val bitmap: Bitmap?): IBookContent

data class Fb2BookContent(val text: AnnotatedString?, val extraContent: MutableMap<String, InlineTextContent>): IBookContent
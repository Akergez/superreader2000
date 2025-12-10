package com.example.bottomapp.shared.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString

interface IBookContent

data class PdfBookContent(val bitmap: ImageBitmap?): IBookContent
data class DjvuBookContent(val bitmap: ImageBitmap?): IBookContent

data class Fb2BookContent(val text: AnnotatedString?, val extraContent: MutableMap<String, InlineTextContent>): IBookContent
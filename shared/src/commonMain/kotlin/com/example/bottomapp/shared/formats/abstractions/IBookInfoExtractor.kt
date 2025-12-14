package com.example.bottomapp.shared.formats.abstractions

import java.io.File

interface IBookInfoExtractor<T> {
    fun getAuthor(file: File): String?
}
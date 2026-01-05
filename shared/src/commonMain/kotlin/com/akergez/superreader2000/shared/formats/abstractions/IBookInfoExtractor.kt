package com.akergez.superreader2000.shared.formats.abstractions

import java.io.File

interface IBookInfoExtractor<T> {
    fun getAuthor(file: File): String?
}
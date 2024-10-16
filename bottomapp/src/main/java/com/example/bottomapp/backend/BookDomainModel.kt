package com.example.bottomapp.backend

import java.io.File

enum class FormatType {
    PDF,
    FB2
}

open class Progress(val formatType: FormatType)


interface BookDomainModel<T> where T : Progress{
    fun getProgress(): T
    fun saveProgress(progress: T)
    fun getFile(): File
}
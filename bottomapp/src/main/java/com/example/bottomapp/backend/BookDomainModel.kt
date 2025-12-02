package com.example.bottomapp.backend

import java.io.File

// Re-export from shared module for backward compatibility
typealias FormatType = com.example.bottomapp.shared.backend.domain.FormatType

abstract class Progress(val formatType: FormatType)
{
    abstract fun getProgressInPercent(): Double?
}

interface BookDomainModel<T> where T : Progress{
    fun readProgress(): T
    suspend fun saveProgress(progress: T)
    suspend fun getFile(): File
}
package com.example.bottomapp.shared.formats.abstractions

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper

interface BookDomainModel<T : Progress> {
    fun readProgress(): T

    suspend fun saveProgress(progress: T)

    suspend fun getFile(): FileWrapper

    fun getMetadata(): BookMetaInfo
}
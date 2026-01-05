package com.akergez.superreader2000.shared.formats.abstractions

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper

interface BookDomainModel<T : Progress> {
    fun readProgress(): T

    suspend fun saveProgress(progress: T)

    suspend fun getFile(): FileWrapper

    fun getMetadata(): BookMetaInfo
}
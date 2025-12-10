package com.example.bottomapp.shared.backend

import android.content.Context
import com.example.bottomapp.shared.backend.bookExtractors.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.StorageManager

actual class BookStorage(
    context: Context,
    settingsManager: ISettingsManager,
    adapter: StorageAdapter,
    infoExtractorFactory: BookInfoExtractorFactory,
    previewGeneratorMap: Map<FormatType, IPreviewGenerator>
) : StorageManager<BookMetaInfo>(
    context,
    settingsManager,
    adapter,
    adapter,
    infoExtractorFactory,
    {
        previewGeneratorMap[it.progress!!.formatType]
            ?: throw NotImplementedError("PreviewGenerator for ${it.progress!!.formatType} not found, check your DI config")
    }
)
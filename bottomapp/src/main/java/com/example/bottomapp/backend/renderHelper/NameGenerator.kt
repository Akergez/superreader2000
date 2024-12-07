package com.example.bottomapp.backend.renderHelper

import com.example.bottomapp.backend.BookMetaInfo

object NameGenerator {
    fun jsonMetaFilename(meta: BookMetaInfo): String {
        return "${meta.fileName}_meta.json"
    }

    fun previewFilename(meta: BookMetaInfo): String {
        return ".${meta.fileName}_preview.jpg"
    }
} // todo прикольно было бы сохранять все связанное с файлом в отдельную директорию,
// тогда названия файлов будут просто preview.jpg и meta.json
// зато директория ".${meta.fileName.replace('.', '_'}"
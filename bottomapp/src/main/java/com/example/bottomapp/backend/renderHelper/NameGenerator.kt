package com.example.bottomapp.backend.renderHelper

import com.example.bottomapp.backend.BookMetaInfo

object NameGenerator {
    fun jsonMetaFilename(fileName: String): String {
        return "${fileName}_meta.json"
    }

    fun previewFilename(fileName: String): String {
        return "${fileName}_preview.jpg"
    }
} // todo прикольно было бы сохранять все связанное с файлом в отдельную директорию,
// тогда названия файлов будут просто preview.jpg и meta.json
// зато директория ".${meta.fileName.replace('.', '_'}"
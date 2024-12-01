package com.example.bottomapp.backend.RenderHelper

import com.example.bottomapp.backend.BookMetaInfo

object NameGenerator {
    fun jsonMetaFilename(meta: BookMetaInfo): String {
        return "${meta.fileName}_meta.json"
    }

    fun previewFilename(meta: BookMetaInfo): String {
        return "${meta.fileName}_preview.jpg"
    }
}
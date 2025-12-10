package com.example.bottomapp.shared.backend.storage

interface INameProvider<T> {
    fun jsonMetaFilename(bundleName: String): String
    fun previewFilename(bundleName: String): String
    fun fileName(bundleName: String, meta: T): String
}
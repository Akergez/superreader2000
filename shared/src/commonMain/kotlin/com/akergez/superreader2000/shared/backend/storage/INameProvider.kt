package com.akergez.superreader2000.shared.backend.storage

interface INameProvider<T> {
    fun jsonMetaFilename(bundleName: String): String
    fun previewFilename(bundleName: String): String
    fun fileName(bundleName: String, meta: T): String
}
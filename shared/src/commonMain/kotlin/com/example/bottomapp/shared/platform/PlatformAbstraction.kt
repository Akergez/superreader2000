package com.example.bottomapp.shared.platform


/**
 * Platform abstraction for file I/O operations
 * Implement differently for Android vs Desktop
 */
expect class FileProvider {
    suspend fun loadFileBytes(path: String): ByteArray
    suspend fun saveFileBytes(path: String, data: ByteArray)
    suspend fun deleteFile(path: String)
    fun getAppCachePath(): String
    fun getAppDocumentsPath(): String
}

/**
 * Platform abstraction for preferences storage
 */
expect class PreferencesStore {
    suspend fun getString(key: String, default: String = ""): String
    suspend fun putString(key: String, value: String)
    suspend fun getInt(key: String, default: Int = 0): Int
    suspend fun putInt(key: String, value: Int)
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun removeKey(key: String)
}

/**
 * Platform abstraction for image rendering/bitmap operations
 */
expect class BitmapProvider {
    fun createBitmap(width: Int, height: Int): PlatformBitmap
    fun bitmapToByteArray(bitmap: PlatformBitmap): ByteArray
}

/**
 * Platform-specific bitmap representation
 */
expect class PlatformBitmap


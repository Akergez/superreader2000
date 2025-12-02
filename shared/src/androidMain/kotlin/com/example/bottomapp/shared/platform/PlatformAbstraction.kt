package com.example.bottomapp.shared.platform

import android.content.Context
import android.graphics.Bitmap
import android.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual class FileProvider(private val context: Context) {
    actual suspend fun loadFileBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        File(path).readBytes()
    }

    actual suspend fun saveFileBytes(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        File(path).parentFile?.mkdirs()
        File(path).writeBytes(data)
    }

    actual suspend fun deleteFile(path: String): Unit = withContext(Dispatchers.IO) {
        File(path).delete()
    }

    actual fun getAppCachePath(): String = context.cacheDir.absolutePath

    actual fun getAppDocumentsPath(): String = context.filesDir.absolutePath
}

actual class PreferencesStore(private val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    actual suspend fun getString(key: String, default: String): String = withContext(Dispatchers.IO) {
        prefs.getString(key, default) ?: default
    }

    actual suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, value).apply()
    }

    actual suspend fun getInt(key: String, default: Int): Int = withContext(Dispatchers.IO) {
        prefs.getInt(key, default)
    }

    actual suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(key, value).apply()
    }

    actual suspend fun getBoolean(key: String, default: Boolean): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(key, default)
    }

    actual suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(key, value).apply()
    }

    actual suspend fun removeKey(key: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(key).apply()
    }
}

actual class BitmapProvider {
    actual fun createBitmap(width: Int, height: Int): PlatformBitmap {
        return PlatformBitmap(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888))
    }

    actual fun bitmapToByteArray(bitmap: PlatformBitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}

actual class PlatformBitmap(val bitmap: Bitmap)


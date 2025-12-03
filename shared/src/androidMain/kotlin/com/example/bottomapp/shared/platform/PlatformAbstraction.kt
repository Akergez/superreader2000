package com.example.bottomapp.shared.platform

import android.content.Context
import android.graphics.Bitmap
import android.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.content.edit

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
    actual suspend fun getString(key: String, default: String): String {
        TODO("Not yet implemented")
    }

    actual suspend fun putString(key: String, value: String) {
    }

    actual suspend fun getInt(key: String, default: Int): Int {
        TODO("Not yet implemented")
    }

    actual suspend fun putInt(key: String, value: Int) {
    }

    actual suspend fun getBoolean(key: String, default: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual suspend fun putBoolean(key: String, value: Boolean) {
    }

    actual suspend fun removeKey(key: String) {
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


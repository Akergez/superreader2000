package com.example.bottomapp.shared.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.util.prefs.Preferences

actual class FileProvider {
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

    actual fun getAppCachePath(): String {
        val appDataDir = File(System.getProperty("user.home"), ".pdfreaderkmp/cache")
        appDataDir.mkdirs()
        return appDataDir.absolutePath
    }

    actual fun getAppDocumentsPath(): String {
        val appDataDir = File(System.getProperty("user.home"), ".pdfreaderkmp/documents")
        appDataDir.mkdirs()
        return appDataDir.absolutePath
    }
}

actual class PreferencesStore {
    private val prefs = Preferences.userRoot().node("com/example/pdfreaderkmp")

    actual suspend fun getString(key: String, default: String): String = withContext(Dispatchers.IO) {
        prefs.get(key, default)
    }

    actual suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.put(key, value)
    }

    actual suspend fun getInt(key: String, default: Int): Int = withContext(Dispatchers.IO) {
        prefs.getInt(key, default)
    }

    actual suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        prefs.putInt(key, value)
    }

    actual suspend fun getBoolean(key: String, default: Boolean): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(key, default)
    }

    actual suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        prefs.putBoolean(key, value)
    }

    actual suspend fun removeKey(key: String) = withContext(Dispatchers.IO) {
        prefs.remove(key)
    }
}

actual class BitmapProvider {
    actual fun createBitmap(width: Int, height: Int): PlatformBitmap {
        return PlatformBitmap(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))
    }

    actual fun bitmapToByteArray(bitmap: PlatformBitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(bitmap.image, "png", stream)
        return stream.toByteArray()
    }
}

actual class PlatformBitmap(val image: BufferedImage)


package com.example.bottomapp.shared.backend.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

/**
 * Desktop implementation of settings store using Properties file
 */
actual class PreferencesStore : ISettingsStore {
    private val prefsFile = File(System.getProperty("user.home"), ".config/pdfreaderr/prefs.properties")
    private val properties = Properties()

    init {
        if (prefsFile.exists()) {
            prefsFile.inputStream().use { input ->
                properties.load(input)
            }
        }
    }

    private fun ensureDirectoryExists() {
        prefsFile.parentFile?.mkdirs()
    }

    private fun save() {
        ensureDirectoryExists()
        prefsFile.outputStream().use { output ->
            properties.store(output, "PDF Reader Preferences")
        }
    }

    actual override suspend fun getString(key: String, default: String): String = withContext(Dispatchers.IO) {
        properties.getProperty(key, default)
    }

    actual override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        properties.setProperty(key, value)
        save()
    }

    actual override suspend fun getInt(key: String, default: Int): Int = withContext(Dispatchers.IO) {
        properties.getProperty(key, default.toString()).toIntOrNull() ?: default
    }

    actual override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        properties.setProperty(key, value.toString())
        save()
    }

    actual override suspend fun getBoolean(key: String, default: Boolean): Boolean = withContext(Dispatchers.IO) {
        properties.getProperty(key, default.toString()).toBooleanStrictOrNull() ?: default
    }

    actual override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        properties.setProperty(key, value.toString())
        save()
    }

    actual override suspend fun removeKey(key: String) = withContext(Dispatchers.IO) {
        properties.remove(key)
        save()
    }
}


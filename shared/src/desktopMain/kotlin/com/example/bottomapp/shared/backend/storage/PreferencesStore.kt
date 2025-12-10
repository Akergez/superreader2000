package com.example.bottomapp.shared.backend.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

/**
 * Desktop implementation of settings store using Properties file
 */
actual class PreferencesStore {
    private val prefsFile =
        File(System.getProperty("user.home"), ".config/pdfreaderr/prefs.properties")
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

    actual fun getString(key: String, default: String): String =
        properties.getProperty(key, default)

    actual fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        save()
    }

    actual fun getInt(key: String, default: Int): Int {
        return properties.getProperty(key, default.toString()).toIntOrNull() ?: default
    }

    actual fun putInt(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        save()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return properties.getProperty(key, default.toString()).toBooleanStrictOrNull() ?: default
    }

    actual fun putBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        save()
    }

    actual fun removeKey(key: String) {
        properties.remove(key)
        save()
    }
}


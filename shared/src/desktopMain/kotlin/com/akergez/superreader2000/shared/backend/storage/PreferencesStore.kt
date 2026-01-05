package com.akergez.superreader2000.shared.backend.storage

import java.io.File
import java.util.Properties

actual class PreferencesStore {
    private val prefsFile =
        File(System.getProperty("user.home"), ".config/superReader2000/prefs.properties")
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

    actual fun removeKey(key: String) {
        properties.remove(key)
        save()
    }
}


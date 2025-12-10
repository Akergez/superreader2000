package com.example.bottomapp.shared.backend.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

/**
 * Android implementation of settings store using SharedPreferences
 */
actual class PreferencesStore(private val context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    actual fun getString(key: String, default: String): String {
        return sharedPreferences.getString(key, default) ?: default
    }

    actual fun putString(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    actual fun getInt(key: String, default: Int): Int {
        return sharedPreferences.getInt(key, default)
    }

    actual fun putInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    actual fun removeKey(key: String) {
        sharedPreferences.edit { remove(key) }
    }
}


package com.example.bottomapp.shared.backend.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of settings store using SharedPreferences
 */
actual class PreferencesStore(private val context: Context) : ISettingsStore {
    private val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    actual override suspend fun getString(key: String, default: String): String = withContext(Dispatchers.IO) {
        sharedPreferences.getString(key, default) ?: default
    }

    actual override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    actual override suspend fun getInt(key: String, default: Int): Int = withContext(Dispatchers.IO) {
        sharedPreferences.getInt(key, default)
    }

    actual override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    actual override suspend fun getBoolean(key: String, default: Boolean): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(key, default)
    }

    actual override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    actual override suspend fun removeKey(key: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(key).apply()
    }
}


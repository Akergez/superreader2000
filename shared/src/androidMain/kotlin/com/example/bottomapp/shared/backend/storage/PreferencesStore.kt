package com.example.bottomapp.shared.backend.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

actual class PreferencesStore(private val context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    actual fun getString(key: String, default: String): String {
        return sharedPreferences.getString(key, default) ?: default
    }

    actual fun putString(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }
    actual fun removeKey(key: String) {
        sharedPreferences.edit { remove(key) }
    }
}


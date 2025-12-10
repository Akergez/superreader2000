package com.example.bottomapp.shared.backend.storage

/**
 * Platform abstraction for preferences storage
 * Implement differently for Android vs Desktop
 */
expect class PreferencesStore {
     fun getString(key: String, default: String): String
     fun putString(key: String, value: String)
     fun getInt(key: String, default: Int): Int
     fun putInt(key: String, value: Int)
     fun getBoolean(key: String, default: Boolean): Boolean
     fun putBoolean(key: String, value: Boolean)
     fun removeKey(key: String)
}


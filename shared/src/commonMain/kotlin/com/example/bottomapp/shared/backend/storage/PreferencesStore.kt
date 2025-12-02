package com.example.bottomapp.shared.backend.storage

/**
 * Platform abstraction for preferences storage
 * Implement differently for Android vs Desktop
 */
expect class PreferencesStore : ISettingsStore {
    override suspend fun getString(key: String, default: String): String
    override suspend fun putString(key: String, value: String)
    override suspend fun getInt(key: String, default: Int): Int
    override suspend fun putInt(key: String, value: Int)
    override suspend fun getBoolean(key: String, default: Boolean): Boolean
    override suspend fun putBoolean(key: String, value: Boolean)
    override suspend fun removeKey(key: String)
}


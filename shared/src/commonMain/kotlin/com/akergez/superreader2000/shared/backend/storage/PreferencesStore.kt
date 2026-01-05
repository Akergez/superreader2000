package com.akergez.superreader2000.shared.backend.storage

expect class PreferencesStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun removeKey(key: String)
}


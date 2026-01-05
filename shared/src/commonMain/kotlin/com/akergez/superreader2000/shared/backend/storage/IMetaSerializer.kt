package com.akergez.superreader2000.shared.backend.storage

interface IDeserializer<T> {
    fun deserialize(jsonString: String): T
    fun serialize(obj: T): String
}
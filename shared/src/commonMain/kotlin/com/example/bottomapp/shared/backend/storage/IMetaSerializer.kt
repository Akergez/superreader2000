package com.example.bottomapp.shared.backend.storage

interface IDeserializer<T> {
    fun deserialize(jsonString: String): T
    fun serialize(obj: T): String
}
package com.example.bottomapp.backend.exceptions

class StorageNotSetException : Exception {
    constructor(message: String) : super("Storage not set $message")

    constructor() : super("Storage not set")
}
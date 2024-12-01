package com.example.bottomapp.backend.exceptions

class InvalidStorageDirectoryException : Exception {
    constructor(message: String) : super("Invalid storage directory $message")

    constructor() : super("Invalid storage directory")
}
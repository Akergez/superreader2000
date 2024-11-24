package com.example.bottomapp.backend.exceptions

class FailedToCreateFileException : Exception {
    constructor(message: String) : super("Failed to create destination file $message")

    constructor() : super("Failed to create destination file")
}
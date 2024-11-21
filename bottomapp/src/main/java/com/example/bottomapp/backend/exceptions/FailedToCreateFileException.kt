package com.example.bottomapp.backend.exceptions

class FailedToCreateFileException : Exception {
    constructor() : super("Failed to create destination file")
}
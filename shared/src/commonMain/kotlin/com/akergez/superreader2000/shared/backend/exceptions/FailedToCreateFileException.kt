package com.akergez.superreader2000.shared.backend.exceptions

class FailedToCreateFileException : Exception {
    constructor(message: String) : super("Failed to create destination file $message")

    constructor() : super("Failed to create destination file")
}


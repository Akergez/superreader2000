package com.akergez.superreader2000.shared.backend.exceptions

class StorageNotSetException : Exception {
    constructor(message: String) : super("Storage not set $message")

    constructor() : super("Storage not set")
}
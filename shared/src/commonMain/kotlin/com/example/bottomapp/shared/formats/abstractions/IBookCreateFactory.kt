package com.example.bottomapp.shared.formats.abstractions

interface IBookCreateFactory {
    fun createProgress(): Progress
    fun getNameFromFileName(fileName: String): String
}
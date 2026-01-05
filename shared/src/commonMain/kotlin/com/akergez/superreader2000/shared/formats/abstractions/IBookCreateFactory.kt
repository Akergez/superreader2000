package com.akergez.superreader2000.shared.formats.abstractions

interface IBookCreateFactory {
    fun createProgress(): Progress
    fun getNameFromFileName(fileName: String): String
}
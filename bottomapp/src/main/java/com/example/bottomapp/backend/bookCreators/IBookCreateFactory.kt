package com.example.bottomapp.backend.bookCreators

import com.example.bottomapp.backend.Progress

interface IBookCreateFactory {
    fun createProgress(): Progress
    fun getNameFromFileName(fileName: String): String
}
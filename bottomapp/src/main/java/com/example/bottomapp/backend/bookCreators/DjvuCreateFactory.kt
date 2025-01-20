package com.example.bottomapp.backend.bookCreators

import com.example.bottomapp.backend.DjvuProgress
import com.example.bottomapp.backend.Progress

class DjvuCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return DjvuProgress(0, null)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".djvu", "")
    }
}
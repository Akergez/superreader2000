package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.IBookCreateFactory
import com.example.bottomapp.shared.formats.abstractions.Progress

class DjvuCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return DjvuProgress(0, null)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".djvu", "")
    }
}
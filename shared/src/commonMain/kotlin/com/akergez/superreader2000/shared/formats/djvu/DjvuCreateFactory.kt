package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.IBookCreateFactory
import com.akergez.superreader2000.shared.formats.abstractions.Progress

class DjvuCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return DjvuProgress(0, null)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".djvu", "")
    }
}
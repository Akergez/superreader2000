package com.akergez.superreader2000.shared.formats.fb2

import com.akergez.superreader2000.shared.formats.abstractions.IBookCreateFactory
import com.akergez.superreader2000.shared.formats.abstractions.Progress

class Fb2CreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return Fb2Progress(0, 0)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".fb2", "")
    }
}
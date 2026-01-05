package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.formats.abstractions.IBookCreateFactory
import com.akergez.superreader2000.shared.formats.abstractions.Progress

class PdfCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return PdfProgress(0, 0)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".pdf", "")
    }
}
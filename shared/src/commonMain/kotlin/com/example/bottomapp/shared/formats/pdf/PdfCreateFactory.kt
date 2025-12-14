package com.example.bottomapp.shared.formats.pdf

import com.example.bottomapp.shared.formats.abstractions.IBookCreateFactory
import com.example.bottomapp.shared.formats.abstractions.Progress

class PdfCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return PdfProgress(0, 0)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".pdf", "")
    }
}
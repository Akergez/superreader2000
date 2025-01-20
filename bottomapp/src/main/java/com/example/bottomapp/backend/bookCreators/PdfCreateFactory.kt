package com.example.bottomapp.backend.bookCreators

import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.Progress

class PdfCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return PdfProgress(0, null)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".pdf", "")
    }
}
package com.example.bottomapp.backend.bookCreators

import com.example.bottomapp.shared.backend.domain.PdfProgress
import com.example.bottomapp.shared.backend.domain.Progress

class PdfCreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return PdfProgress(0, 0)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".pdf", "")
    }
}
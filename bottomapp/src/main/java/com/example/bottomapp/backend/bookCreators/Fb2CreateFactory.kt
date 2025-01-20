package com.example.bottomapp.backend.bookCreators

import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.Progress

class Fb2CreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return Fb2Progress(0, null)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".fb2", "")
    }
}
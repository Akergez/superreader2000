package com.example.bottomapp.backend.bookCreators
import com.example.bottomapp.shared.backend.domain.Fb2Progress
import com.example.bottomapp.shared.backend.domain.Progress

class Fb2CreateFactory : IBookCreateFactory {
    override fun createProgress(): Progress {
        return Fb2Progress(0, 0)
    }

    override fun getNameFromFileName(fileName: String): String {
        return fileName.replace(".fb2", "")
    }
}
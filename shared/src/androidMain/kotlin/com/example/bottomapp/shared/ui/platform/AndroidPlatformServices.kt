package com.example.bottomapp.shared.ui.platform

import android.content.Context
import android.widget.Toast

/**
 * Android implementation of platform services.
 */
class AndroidPlatformServices(
    private val context: Context
) : IPlatformServices {

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override suspend fun pickFile(fileFilter: String): String? {
        // TODO: Implement file picker using Android intent
        return null
    }
}

/**
 * Android implementation of file service.
 */
class AndroidFileService(
    private val context: Context
) : IFileService {

    override fun getFileName(path: String): String {
        return path.substringAfterLast('/')
    }

    override suspend fun readFileBytes(path: String): ByteArray {
        // TODO: Implement file reading
        return ByteArray(0)
    }

    override suspend fun exists(path: String): Boolean {
        // TODO: Implement file existence check
        return false
    }
}


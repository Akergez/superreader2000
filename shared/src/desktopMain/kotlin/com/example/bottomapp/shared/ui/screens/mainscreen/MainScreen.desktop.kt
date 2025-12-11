package com.example.bottomapp.shared.ui.screens.mainscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * Desktop implementation of StorageDirectoryPicker
 */
actual class StorageDirectoryPicker(
    private val onDirectorySelected: (String) -> Unit,
    private val onPermissionGranted: suspend () -> Unit
) {
    actual fun launch() {
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Select Book Storage Directory"
            }

            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedDir = fileChooser.selectedFile
                onDirectorySelected(selectedDir.absolutePath)

                // Trigger refresh in coroutine
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    onPermissionGranted()
                }
            }
        }
    }
}

/**
 * Desktop implementation of storage directory picker
 */
@Composable
actual fun rememberStorageDirectoryPicker(
    onDirectorySelected: (String) -> Unit,
    onPermissionGranted: suspend () -> Unit
): StorageDirectoryPicker {
    return remember(onDirectorySelected) {
        StorageDirectoryPicker(onDirectorySelected, onPermissionGranted)
    }
}

/**
 * Desktop implementation of string resource getter
 * TODO: Implement proper resource loading for Desktop
 */
@Composable
actual fun getStringResource(key: String): String {
    return when (key) {
        "choose_storage" -> "Choose Storage"
        "choose_storage_description" -> "Please select a directory where your books are stored"
        "choose" -> "Choose"
        else -> key
    }
}

/**
 * Desktop implementation of URL encoding
 */
actual fun urlEncode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

/**
 * Desktop implementation of URL decoding
 */
actual fun urlDecode(value: String): String {
    return URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
}


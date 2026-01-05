package com.akergez.superreader2000.shared.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class BookLauncher(
    private val launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    actual fun launch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"/*        putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("application/pdf", "text/fb2+xml", "application/fb2")
                )*/
        }
        launcher.launch(intent)
    }
}

@Composable
actual fun rememberBookPickerLauncher(onImageSelected: (String) -> Unit): BookLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                onImageSelected(uri.toString())
            }
        }
    }

    return remember(launcher) {
        BookLauncher(launcher)
    }
}
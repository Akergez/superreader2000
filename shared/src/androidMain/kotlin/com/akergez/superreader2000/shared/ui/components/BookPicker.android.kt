package com.akergez.superreader2000.shared.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class BookLauncher(
    private val launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    actual fun launch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        launcher.launch(intent)
    }
}

@Composable
actual fun rememberBookPickerLauncher(onImageSelected: (String) -> Unit): BookLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // Some document providers grant only temporary access; copying can
                    // still proceed while the activity grant is alive.
                }
                onImageSelected(uri.toString())
            }
        }
    }

    return remember(launcher) {
        BookLauncher(launcher)
    }
}

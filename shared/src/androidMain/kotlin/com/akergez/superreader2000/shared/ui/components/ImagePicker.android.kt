package com.akergez.superreader2000.shared.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class ImagePickerLauncher(
    private val launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    actual fun launch() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        launcher.launch(intent)
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (String) -> Unit
): ImagePickerLauncher {
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
        ImagePickerLauncher(launcher)
    }
}


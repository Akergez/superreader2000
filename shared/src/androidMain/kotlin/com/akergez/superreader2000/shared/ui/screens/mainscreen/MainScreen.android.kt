package com.akergez.superreader2000.shared.ui.screens.mainscreen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Android implementation of StorageDirectoryPicker
 */
actual class StorageDirectoryPicker(
    private val launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    private val onDirectorySelected: (String) -> Unit,
    private val onPermissionGranted: suspend () -> Unit,
    private val context: android.content.Context
) {
    actual fun launch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        launcher.launch(intent)
    }

    internal fun handleResult(uri: Uri?) {
        uri?.let { selectedUri ->
            // Grant persistable permissions
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onDirectorySelected(selectedUri.toString())

            // Trigger refresh in coroutine
            CoroutineScope(Dispatchers.Main).launch {
                onPermissionGranted()
            }
        }
    }
}

/**
 * Android implementation of storage directory picker
 */
@Composable
actual fun rememberStorageDirectoryPicker(
    onDirectorySelected: (String) -> Unit,
    onPermissionGranted: suspend () -> Unit
): StorageDirectoryPicker {
    val context = LocalContext.current

    val picker = remember { mutableListOf<StorageDirectoryPicker>() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            picker.firstOrNull()?.handleResult(result.data?.data)
        }
    }

    return remember(launcher, onDirectorySelected) {
        StorageDirectoryPicker(launcher, onDirectorySelected, onPermissionGranted, context).also {
            picker.clear()
            picker.add(it)
        }
    }
}

actual fun urlEncode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

actual fun urlDecode(value: String): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        URLDecoder.decode(value, StandardCharsets.UTF_8)
    } else {
        @Suppress("DEPRECATION")
        URLDecoder.decode(value)
    }
}


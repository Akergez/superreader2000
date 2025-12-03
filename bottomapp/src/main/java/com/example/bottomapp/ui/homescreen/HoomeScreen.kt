package com.example.bottomapp.ui.homescreen

import EditBookModal
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavController
import com.example.bottomapp.R
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.constants.FileType
import com.example.bottomapp.constants.toFileExtension
import com.example.bottomapp.constants.toMimeType
import com.example.bottomapp.ui.homescreen.components.BookItem
import com.example.bottomapp.ui.homescreen.components.TopEditBar
import com.example.bottomapp.ui.mainscreen.BottomNavigationBar
import com.example.bottomapp.ui.mainscreen.Screen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (BookStorageEntry) -> Unit,
    hoomeScreenViewModel: HoomeScreenViewModel = koinViewModel(),
    navController: NavController
) {
    val state by hoomeScreenViewModel.hoomeScreenState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val extension = getFileType(result.data?.data.toString())
                    val mimeType = context.contentResolver.getType(uri)
                    val supportedTypesList = FileType.entries.filter { it != FileType.unknown }
                        .map { it to it.toFileExtension() }
                    val supportedMimeTypesList = FileType.entries.filter { it != FileType.unknown }
                        .map { it to it.toMimeType() }
                    if (supportedTypesList.map { it.second }.contains(extension)) {
                        hoomeScreenViewModel.addBook(
                            uri,
                            context,
                            supportedTypesList.firstOrNull() { it.second == extension }?.first
                                ?: FileType.unknown
                        )
                    } else if (supportedMimeTypesList.map { it.second }.flatten()
                            .contains(mimeType)
                    ) {
                        val type = supportedMimeTypesList
                            .firstOrNull { it.second.contains(mimeType) }?.first
                            ?: FileType.unknown
                        hoomeScreenViewModel.addBook(uri, context, type)
                    } else {
                        Toast.makeText(
                            context,
                            getString(context, R.string.error_not_supported_format),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    val books by hoomeScreenViewModel.books.collectAsState()
    val selectedBooks by hoomeScreenViewModel.selectedBooks.collectAsState()
    val haptic = LocalHapticFeedback.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        hoomeScreenViewModel.dumpBooks()
    }

    Scaffold(floatingActionButton = {
        if (selectedBooks.size == 1) {
            FloatingActionButton(onClick = {
                hoomeScreenViewModel.updateEditingBook(selectedBooks.first())
                hoomeScreenViewModel.clearSelectedBooks()
            }) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = getString(LocalContext.current, R.string.edit_book)
                )
            }
        } else if (selectedBooks.isEmpty()) {
            FloatingActionButton(onClick = { launcher.launch(openFilePicker()) }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = getString(LocalContext.current, R.string.add_book)
                )
            }
        }
    }, bottomBar = { BottomNavigationBar(navController, Screen.HOME_SCREEN) }, topBar = {
        if (selectedBooks.isNotEmpty()) {
            TopEditBar(onClose = { hoomeScreenViewModel.clearSelectedBooks() }, onDelete = {
                hoomeScreenViewModel.onDeleteBooks(selectedBooks, context)
            }, selectedCount = selectedBooks.size
            )
        } else {
            TopAppBar(
                title = {
                    Text(
                        text = getString(LocalContext.current, R.string.my_books),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }, scrollBehavior = scrollBehavior
            )
        }
    }, modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding), state = lazyListState
        ) {
            items(books.sortedBy { x -> x.title }, key = { it.bundleName }) { bookLink ->
                val isSelected = bookLink in selectedBooks
                val isSelectedAny = selectedBooks.isNotEmpty()
                BookItem(book = bookLink, isSelected = isSelected, onItemClick = { book ->
                    if (isSelectedAny) {
                        hoomeScreenViewModel.updateSelectedBook(bookLink)
                    } else {
                        onItemClick(book)
                        hoomeScreenViewModel.clearTemporaryState()
                    }
                }, onLongClick = {
                    hoomeScreenViewModel.updateSelectedBook(bookLink)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                })
            }
        }
    }

    state.editingBook?.let { book ->
        val toast = Toast.makeText(context, "", Toast.LENGTH_SHORT)
        EditBookModal(
            onClose = {
                coroutineScope.launch {
                    hoomeScreenViewModel.updateEditingBook(null)
                }
            },
            onSave = { updatedBook, changed, previewChanged ->
                if (updatedBook.bundleName.isEmpty()) {
                    toast.setText(getString(context, R.string.error_book_title_empty))
                    toast?.show()
                } else if (books.all { x -> x == book || x.bundleName != updatedBook.bundleName }) {
                    hoomeScreenViewModel.onSaveEditModal(
                        book, updatedBook, changed, previewChanged, context
                    )
                } else {
                    toast.setText(getString(context, R.string.error_book_with_title_already_exists))
                    toast?.show()
                }
            },
            editModalViewModel = koinViewModel(key = Uuid.random().toString(),
                parameters = { parametersOf(book) }),
            isVisible = true
        )
    }
}

private fun getFileType(fileName: String): String {
    return fileName.substringAfterLast('.', "").lowercase()
}

private fun openFilePicker(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"/*        putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("application/pdf", "text/fb2+xml", "application/fb2")
                )*/
    }
}

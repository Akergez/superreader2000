package com.akergez.superreader2000.shared.ui.screens.homescreen

import EditBookModal
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
import androidx.compose.ui.platform.LocalHapticFeedback
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.constants.toFileExtension
import com.akergez.superreader2000.shared.constants.toMimeType
import com.akergez.superreader2000.shared.formats.abstractions.FileType
import com.akergez.superreader2000.shared.platform.MessageHandler
import com.akergez.superreader2000.shared.ui.components.BookItem
import com.akergez.superreader2000.shared.ui.components.TopEditBar
import com.akergez.superreader2000.shared.ui.components.rememberBookPickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.add_book
import superreader2000.shared.generated.resources.edit_book
import superreader2000.shared.generated.resources.error_book_title_empty
import superreader2000.shared.generated.resources.error_book_with_title_already_exists
import superreader2000.shared.generated.resources.error_not_supported_format
import superreader2000.shared.generated.resources.my_books
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (BookStorageEntry) -> Unit,
    messageHandler: MessageHandler = koinInject(),
    hoomeScreenViewModel: HoomeScreenViewModel = koinViewModel(),
    bottomBar: @Composable () -> Unit = {}
) {
    val sErrorNotSupported = stringResource(Res.string.error_not_supported_format)
    val sErrorBookEmpty = stringResource(Res.string.error_book_title_empty)
    val sErrorBookExists = stringResource(Res.string.error_book_with_title_already_exists)
    val sAddBook = stringResource(Res.string.add_book)
    val sEditBook = stringResource(Res.string.edit_book)
    val sMyBooks = stringResource(Res.string.my_books)
    val state by hoomeScreenViewModel.hoomeScreenState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val launcher =
        rememberBookPickerLauncher { uri ->
            val extension = getFileType(uri)
            val mimeType = "application/$extension"
            val supportedTypesList = FileType.entries.filter { it != FileType.unknown }
                .map { it to it.toFileExtension() }
            val supportedMimeTypesList = FileType.entries.filter { it != FileType.unknown }
                .map { it to it.toMimeType() }
            if (supportedTypesList.map { it.second }.contains(extension)) {
                hoomeScreenViewModel.addBook(
                    uri,
                    supportedTypesList.firstOrNull() { it.second == extension }?.first
                        ?: FileType.unknown
                )
            } else if (supportedMimeTypesList.map { it.second }.flatten()
                    .contains(mimeType)
            ) {
                val type = supportedMimeTypesList
                    .firstOrNull { it.second.contains(mimeType) }?.first
                    ?: FileType.unknown
                hoomeScreenViewModel.addBook(uri, type)
            } else {
                messageHandler.showLongMessage(sErrorNotSupported)
            }


        }
    val books by hoomeScreenViewModel.books.collectAsState()
    val selectedBooks by hoomeScreenViewModel.selectedBooks.collectAsState()
    val haptic = LocalHapticFeedback.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        hoomeScreenViewModel.dumpBooks()
    }

    Scaffold(
        floatingActionButton = {
            if (selectedBooks.size == 1) {
                FloatingActionButton(onClick = {
                    hoomeScreenViewModel.updateEditingBook(selectedBooks.first())
                    hoomeScreenViewModel.clearSelectedBooks()
                }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = sEditBook
                    )
                }
            } else if (selectedBooks.isEmpty()) {
                FloatingActionButton(onClick = { launcher.launch() }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = sAddBook
                    )
                }
            }
        }, bottomBar = bottomBar, topBar = {
            if (selectedBooks.isNotEmpty()) {
                TopEditBar(
                    onClose = { hoomeScreenViewModel.clearSelectedBooks() }, onDelete = {
                        hoomeScreenViewModel.onDeleteBooks(selectedBooks)
                    }, selectedCount = selectedBooks.size
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = sMyBooks,
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
        EditBookModal(
            onClose = {
                coroutineScope.launch {
                    hoomeScreenViewModel.updateEditingBook(null)
                }
            },
            onSave = { updatedBook, changed, previewChanged ->
                if (updatedBook.bundleName.isEmpty()) {
                    messageHandler.showLongMessage(sErrorBookEmpty)
                } else if (books.all { x -> x == book || x.bundleName != updatedBook.bundleName }) {
                    hoomeScreenViewModel.onSaveEditModal(
                        book, updatedBook, changed, previewChanged
                    )
                } else {
                    messageHandler.showLongMessage(sErrorBookExists)
                }
            },
            editModalViewModel = koinViewModel(
                key = Uuid.random().toString(),
                parameters = { parametersOf(book) }),
            isVisible = true
        )
    }
}

private fun getFileType(fileName: String): String {
    return fileName.substringAfterLast('.', "").lowercase()
}

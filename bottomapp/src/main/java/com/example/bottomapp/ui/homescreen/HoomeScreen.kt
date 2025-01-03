package com.example.bottomapp.ui.homescreen

import EditBookModal
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavController
import com.example.bottomapp.R
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.ui.homescreen.components.BookItem
import com.example.bottomapp.ui.homescreen.components.TopEditBar
import com.example.bottomapp.ui.mainscreen.BottomNavigationBar
import com.example.bottomapp.ui.mainscreen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun HomeScreen(
    onItemClick: (BookStorageEntry) -> Unit,
    hoomeScreenViewModel: HoomeScreenViewModel = koinViewModel(),
    navController: NavController
) {
    hoomeScreenViewModel.dumpBooks()
    val state by hoomeScreenViewModel.hoomeScreenState.collectAsState()
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    Log.d("color", "HomeScreen: ${MaterialTheme.colorScheme.background}")
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val type = getFileType(result.data?.data.toString());
                when (type) {
                    "pdf", "fb2" -> {
                        result.data?.data?.also { uri ->
                            hoomeScreenViewModel.addBook(uri, context)
                        }
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            "Только файлы формата PDF и FB2 поддерживаются приложением",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    val books by hoomeScreenViewModel.books.collectAsState()
    val selectedBooks by hoomeScreenViewModel.selectedBooks.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(openFilePicker()) })
            {
                Icon(Icons.Filled.Add, contentDescription = getString(LocalContext.current, R.string.add_book))
            }
        },
        bottomBar = { BottomNavigationBar(navController, Screen.HOME_SCREEN) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = lazyListState
        ) {
            item {
                Text(
                    text = getString(LocalContext.current, R.string.my_books),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            items(books, key = { it.bundleName }) { bookLink ->
                val isSelected = bookLink in selectedBooks
                val isSelectedAny = selectedBooks.isNotEmpty()
                BookItem(
                    book = bookLink,
                    isSelected = isSelected,
                    onItemClick = { book ->
                        if (isSelectedAny) {
                            hoomeScreenViewModel.updateSelectedBook(bookLink)
                        } else{
                            onItemClick(book)
                            hoomeScreenViewModel.clearTemporaryState()
                        }
                    },
                    onLongClick = { hoomeScreenViewModel.updateSelectedBook(bookLink)  }
                )
            }
        }

        if (selectedBooks.isNotEmpty()) {
            TopEditBar(
                onClose = { hoomeScreenViewModel.clearSelectedBooks() },
                onEdit = {
                    hoomeScreenViewModel.updateEditingBook(selectedBooks.first())
                    hoomeScreenViewModel.clearSelectedBooks()
                },
                onDelete = {
                    hoomeScreenViewModel.onDeleteBooks(selectedBooks, context)
                },
                selectedCount = selectedBooks.size
            )
        }
    }

    state.editingBook?.let { book ->
        CoroutineScope(Dispatchers.Default).launch {
            sheetState.show()
        }
        EditBookModal(
            onClose = { hoomeScreenViewModel.updateEditingBook(null) },
            onSave = { updatedBook, changed, previewChanged ->
                hoomeScreenViewModel.onSaveEditModal(book, updatedBook, changed, previewChanged, context)
            },
            editModalViewModel = koinViewModel(key = Uuid.random().toString(), parameters = { parametersOf(book) })
        )
    }

}

private fun getFileType(fileName: String): String {
    return fileName.substringAfterLast('.', "").lowercase()
}

private fun openFilePicker(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }
}
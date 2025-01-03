import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.ui.homescreen.components.AsyncImage
import com.example.bottomapp.ui.homescreen.components.EditModalViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookModal(
    onClose: () -> Unit,
    onSave: (newBook: BookStorageEntry, changed: Boolean, previewChanged: Boolean) -> Unit,
    editModalViewModel: EditModalViewModel = koinInject()
) {
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)
    val state by editModalViewModel.editModalState.collectAsState()
    val book = editModalViewModel.book
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    editModalViewModel.uploadImage(uri)
                }
            }
        }
    val coroutineScope = rememberCoroutineScope()
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
                // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(16.dp)
//                        .pointerInput(Unit) {
//                            detectTapGestures { /* Оставляем пустым, чтобы игнорировать клики */ }
//                        }
            ) {
                Text(
                    text = getString(LocalContext.current, R.string.edit_book),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = state.title,
                    onValueChange = { editModalViewModel.updateTitle(it) },
                    label = { Text(getString(LocalContext.current, R.string.book_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.author,
                    onValueChange = { editModalViewModel.updateAuthor(it) },
                    label = { Text(getString(LocalContext.current, R.string.book_author)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                // Поле загрузки обложки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_PICK).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                                }
                                launcher.launch(intent)
                            }
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                            )
                    ) {
                        AsyncImage(
                            uri = state.previewUri,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.5f // Степень затемнения
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Icon",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = getString(LocalContext.current, R.string.book_cover),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getString(LocalContext.current, R.string.book_cover_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getString(LocalContext.current, R.string.book_cover_formats),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Кнопки
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(
                                book.copy(
                                    bundleName = state.title,
                                    previewUri = state.previewUri,
                                    bookMetaInfo = book.bookMetaInfo.copy(
                                        title = state.title
                                    )
                                ),
                                state.changed,
                                state.previewChanged
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text(getString(LocalContext.current, R.string.save))
                    }

                    Button(
                        onClick = {
                            onClose()
                            coroutineScope.launch { sheetState.hide() }
//                            CoroutineScope(Dispatchers.Default).launch {
//                                sheetState.hide()
//                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 8.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                    ) {
                        Text(getString(LocalContext.current, R.string.cancel))
                    }
                }
            }

        }
    ) {
        // Main content
    }
}

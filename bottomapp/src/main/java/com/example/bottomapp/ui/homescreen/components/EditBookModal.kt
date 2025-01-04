import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.ui.homescreen.components.AsyncImage
import com.example.bottomapp.ui.homescreen.components.EditModalViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditBookModal(
    onClose: () -> Unit,
    draggingFeedback: () -> Unit,
    onSave: (newBook: BookStorageEntry, changed: Boolean, previewChanged: Boolean) -> Unit,
    sheetState: ModalBottomSheetState,
    editModalViewModel: EditModalViewModel = koinInject()
) {
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

    // Без флага он будет вызывать онКлоуз до того как модальное развернется => сбросится книга для редактирования =>
    // => модальное снова пропадет
    var shouldOnCloseCall = false
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.isVisible }
            .collect { isVisible ->
                if (!isVisible && shouldOnCloseCall) {
                    onClose()
                    shouldOnCloseCall = false
                } else if (isVisible) {
                    shouldOnCloseCall = true
                }
            }
    }

    LaunchedEffect(sheetState.progress) {
        editModalViewModel.handleUpperStrip(
            sheetState.progress,
            onExceeding = { editModalViewModel.updateDragging(false) },
            onLowering = { editModalViewModel.updateDragging(true) }
        )
        if (sheetState.currentValue == ModalBottomSheetValue.Expanded
            && sheetState.targetValue != ModalBottomSheetValue.Hidden)
            editModalViewModel.handleVibrationFeedback(sheetState.progress, draggingFeedback, draggingFeedback)
    }

    BackHandler(enabled = sheetState.isVisible) {
        onClose()
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                val color = if (state.isDragging) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                Box(
                    Modifier
                        .height(3.dp)
                        .width(25.dp)
                        .background(
                            color = color,
                            shape = RoundedCornerShape(50),
                        )
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = getString(LocalContext.current, R.string.edit_book),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
//                        .align(Alignment.CenterHorizontally) // Как по мне слева лучше чем по центру
                    ,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = state.title,
                    onValueChange = { editModalViewModel.updateTitle(it) },
                    label = { Text(getString(LocalContext.current, R.string.book_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-5).dp)
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
                            shouldOnCloseCall = false
                            onClose()
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
    ) {  }
}
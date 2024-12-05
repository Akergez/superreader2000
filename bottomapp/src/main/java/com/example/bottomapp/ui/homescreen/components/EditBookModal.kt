package com.example.bottomapp.ui.homescreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.BookMetaInfo
import com.example.bottomapp.backend.BookStorageEntry

@Composable
fun EditBookModal(
    onClose: () -> Unit,
    onSave: (newBook: BookStorageEntry, changed: Boolean) -> Unit,
    editModalViewModel: EditModalViewModel
) {
    val state by editModalViewModel.editModalState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClose() }) // Закрытие при клике на фон
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { /* Оставляем пустым, чтобы игнорировать клики */ }
                }
        ) {
            Text(
                text = "Редактировать книгу",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = { editModalViewModel.updateTitle(it) },
                label = { Text("Название книги") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.author,
                onValueChange = { editModalViewModel.updateAuthor(it) },
                label = { Text("Автор книги") },
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
                AsyncImage(
                    uri = state.previewUri,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // Логика выбора изображения (placeholder для примера)
//                            coverUri.value = null
                        }
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = "Обложка", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "По умолчанию - из файла",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Форматы - jpg, png",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
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
                                previewUri = state.previewUri,
                                bookMetaInfo = book.bookMetaInfo.copy(
                                    title = state.title
                                )
                            ),
                            state.changed
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Сохранить")
                }

                Button(
                    onClick = onClose,
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
                    Text("Отмена")
                }
            }
        }
    }
}

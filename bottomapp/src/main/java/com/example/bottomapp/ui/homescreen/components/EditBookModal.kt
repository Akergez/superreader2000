package com.example.bottomapp.ui.homescreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.BookStorageEntry

@Composable
fun EditBookModal(
    book: BookStorageEntry,
    onClose: () -> Unit,
    onSave: (BookStorageEntry) -> Unit
) {
    var title = remember { mutableStateOf(null) }
    var author = remember { mutableStateOf(book.bookMetaInfo) }
    var coverUri = remember { mutableStateOf(book.previewUri) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = "Редактировать книгу",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )


            // Поле загрузки обложки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Gray, shape = RoundedCornerShape(8.dp))
                        .clickable {
                            // Логика выбора изображения (placeholder для примера)
//                            coverUri.value = null
                        }
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = "Обложка", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "по умолчанию - из файла",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "форматы - jpg, png",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Кнопки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Button(
                    onClick = {
//                        onSave(book.copy(bookMetaInfo = book.bookMetaInfo.copy(title = title, author = author)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

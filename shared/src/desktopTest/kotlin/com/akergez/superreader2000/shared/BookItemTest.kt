package com.akergez.superreader2000.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.ui.components.BookItem
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BookItemTest {

    @Test
    fun bookItem_displaysTitle() = runDesktopComposeUiTest {
        val book = BookStorageEntry(
            title = "The Great Novel",
            previewUri = null,
            bundleName = "the-great-novel",
            bookMetaInfo = BookMetaInfo(title = "The Great Novel", author = "John Doe")
        )

        setContent {
            MaterialTheme {
                BookItem(
                    book = book,
                    onItemClick = {},
                    onLongClick = {},
                    isSelected = false
                )
            }
        }

        onNodeWithText("The Great Novel").assertIsDisplayed()
    }

    @Test
    fun bookItem_displaysAuthor() = runDesktopComposeUiTest {
        val book = BookStorageEntry(
            title = "My Book",
            previewUri = null,
            bundleName = "my-book",
            bookMetaInfo = BookMetaInfo(title = "My Book", author = "Jane Doe")
        )

        setContent {
            MaterialTheme {
                BookItem(
                    book = book,
                    onItemClick = {},
                    onLongClick = {},
                    isSelected = false
                )
            }
        }

        onNodeWithText("Jane Doe").assertIsDisplayed()
    }

    @Test
    fun bookItem_onItemClick_callsCallback() = runDesktopComposeUiTest {
        val book = BookStorageEntry(
            title = "Clicked Book",
            previewUri = null,
            bundleName = "clicked-book",
            bookMetaInfo = BookMetaInfo(title = "Clicked Book")
        )
        var clicked: BookStorageEntry? = null

        setContent {
            MaterialTheme {
                BookItem(
                    book = book,
                    onItemClick = { clicked = it },
                    onLongClick = {},
                    isSelected = false
                )
            }
        }

        onNodeWithText("Clicked Book").performClick()
        assertEquals(book, clicked)
    }
}

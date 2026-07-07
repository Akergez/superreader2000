package com.akergez.superreader2000.shared

import com.akergez.superreader2000.shared.backend.storage.AppDatabase
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.platform.FileUriHandler
import com.akergez.superreader2000.shared.platform.MessageHandler
import com.akergez.superreader2000.shared.ui.screens.homescreen.HoomeScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HoomeScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var vm: HoomeScreenViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = buildTestDatabase()
        vm = HoomeScreenViewModel(
            buildTestBookManager(db),
            emptyMap(),
            MessageHandler(),
            FileUriHandler()
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeBook(name: String) = BookStorageEntry(
        title = name,
        previewUri = null,
        bundleName = name,
        bookMetaInfo = BookMetaInfo(title = name)
    )

    @Test
    fun selectBook_addsToSelectedBooks() {
        val book = makeBook("Book A")
        vm.updateSelectedBook(book)
        assertEquals(listOf(book), vm.selectedBooks.value)
    }

    @Test
    fun selectSameBook_togglesItOff() {
        val book = makeBook("Book A")
        vm.updateSelectedBook(book)
        vm.updateSelectedBook(book)
        assertTrue(vm.selectedBooks.value.isEmpty())
    }

    @Test
    fun selectMultipleBooks_allAppearInSelection() {
        val book1 = makeBook("Book A")
        val book2 = makeBook("Book B")
        vm.updateSelectedBook(book1)
        vm.updateSelectedBook(book2)
        assertEquals(2, vm.selectedBooks.value.size)
        assertTrue(book1 in vm.selectedBooks.value)
        assertTrue(book2 in vm.selectedBooks.value)
    }

    @Test
    fun clearSelectedBooks_emptiesSelection() {
        val book1 = makeBook("Book A")
        val book2 = makeBook("Book B")
        vm.updateSelectedBook(book1)
        vm.updateSelectedBook(book2)
        vm.clearSelectedBooks()
        assertTrue(vm.selectedBooks.value.isEmpty())
    }

    @Test
    fun updateEditingBook_setsEditingState() {
        val book = makeBook("Book A")
        vm.updateEditingBook(book)
        assertEquals(book, vm.hoomeScreenState.value.editingBook)
    }

    @Test
    fun updateEditingBook_null_clearsEditingState() {
        val book = makeBook("Book A")
        vm.updateEditingBook(book)
        vm.updateEditingBook(null)
        assertNull(vm.hoomeScreenState.value.editingBook)
    }

    @Test
    fun clearTemporaryState_resetsSelectionAndEditingBook() {
        val book = makeBook("Book A")
        vm.updateSelectedBook(book)
        vm.updateEditingBook(book)

        vm.clearTemporaryState()

        assertTrue(vm.selectedBooks.value.isEmpty())
        assertNull(vm.hoomeScreenState.value.editingBook)
    }
}

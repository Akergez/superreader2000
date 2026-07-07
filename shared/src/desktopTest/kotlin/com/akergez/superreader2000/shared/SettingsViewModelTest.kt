package com.akergez.superreader2000.shared

import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.storage.AppDatabase
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var settings: FakeSettingsManager
    private lateinit var vm: SettingsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = buildTestDatabase()
        settings = FakeSettingsManager(
            storagePath = "/storage/books",
            appTheme = AppThemeMode.Dark,
            docTheme = DocThemeMode.Invert,
            flipMode = FlipMode.VerticalScroll
        )
        vm = SettingsViewModel(settings, buildTestBookManager(db, settings), testJson)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsStateFromSettingsManager() {
        val state = vm.settingsState.value
        assertEquals("/storage/books", state.storageAddress)
        assertEquals(AppThemeMode.Dark, state.appThemeMode)
        assertEquals(DocThemeMode.Invert, state.docThemeMode)
        assertEquals(FlipMode.VerticalScroll, state.flipMode)
    }

    @Test
    fun updateAppThemeMode_updatesStateAndPersists() {
        vm.updateAppThemeMode(AppThemeMode.Light)
        assertEquals(AppThemeMode.Light, vm.settingsState.value.appThemeMode)
        assertEquals(AppThemeMode.Light, settings.getAppTheme())
    }

    @Test
    fun updateDocThemeMode_updatesStateAndPersists() {
        vm.updateDocThemeMode(DocThemeMode.AsIs)
        assertEquals(DocThemeMode.AsIs, vm.settingsState.value.docThemeMode)
        assertEquals(DocThemeMode.AsIs, settings.getDocumentsTheme())
    }

    @Test
    fun updateFlipMode_updatesStateAndPersists() {
        vm.updateFlipMode(FlipMode.Casual)
        assertEquals(FlipMode.Casual, vm.settingsState.value.flipMode)
        assertEquals(FlipMode.Casual, settings.getFlipMode())
    }

    @Test
    fun updateStorageAddress_updatesStateAndPersists() {
        vm.updateStorageAddress("/new/path")
        assertEquals("/new/path", vm.settingsState.value.storageAddress)
        assertEquals("/new/path", settings.getStoragePath())
    }

    @Test
    fun init_withNoStoragePath_showsEmptyAddress() {
        val emptySettings = FakeSettingsManager(storagePath = null)
        val vmEmpty = SettingsViewModel(emptySettings, buildTestBookManager(db, emptySettings), testJson)
        assertEquals("", vmEmpty.settingsState.value.storageAddress)
    }
}

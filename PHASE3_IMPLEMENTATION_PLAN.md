# Phase 3: UI Migration to Multiplatform - Implementation Plan

## Status: IN PROGRESS ✅

### Overview
Migrate Android ViewModels and UI screens to a shared Compose Multiplatform module with Koin-based state management.

### Current Architecture Analysis

**Android App Structure:**
- ViewModels (5 total):
  - `HoomeScreenViewModel` - Book library management
  - `MainScreenViewModel` - Container screen (empty placeholder)
  - `EditModalViewModel` - Book editing modal
  - `SettingsViewModel` - Settings management
  - `PdfScreenViewModel` - PDF reader screen

- UI Screens:
  - `HoomeScreen` - Book library with grid/list view
  - `MainScreen` - Navigation container with NavHost
  - `SettingsScreen` - App settings
  - Reader screens: `PdfReaderScreen`, `Fb2ReaderScreen`

- Dependencies:
  - AndroidX ViewModel, Lifecycle
  - Android Navigation
  - Material3

**Existing Shared Module:**
- Backend: Domain models, storage, DI setup
- Partial Koin configuration

### Migration Strategy

#### Step 1: Create Shared State Management Layer
- [ ] Create `shared/src/commonMain/ui/state/` directory structure
- [ ] Convert `HoomeScreenViewModel` → `HomeScreenState` (Koin-managed singleton)
- [ ] Convert `SettingsViewModel` → `SettingsState` (Koin-managed singleton)
- [ ] Convert `PdfScreenViewModel` → `PdfReaderState` (Koin-managed factory-scoped)
- [ ] Convert `EditModalViewModel` → `EditBookState` (Koin-managed factory-scoped)
- [ ] Create `MainNavigationState` for navigation/screen routing
- [ ] Move all state classes to commonMain

#### Step 2: Create Multiplatform UI Module Structure
- [ ] `shared/src/commonMain/ui/screens/` - All common composables
- [ ] `shared/src/commonMain/ui/components/` - Reusable UI components
- [ ] `shared/src/commonMain/ui/theme/` - Theme definitions
- [ ] `shared/src/commonMain/ui/navigation/` - Navigation routing
- [ ] `shared/src/commonMain/ui/platform/` - Platform abstraction interfaces
- [ ] `shared/src/androidMain/ui/` - Android-specific implementations
- [ ] `shared/src/desktopMain/ui/` - Desktop-specific implementations

#### Step 3: Create Platform Service Abstractions
- [ ] `PlatformServices` interface for toasts, dialogs, file pickers
- [ ] Android implementation in `shared/src/androidMain/`
- [ ] Desktop implementation in `shared/src/desktopMain/`
- [ ] Register in Koin DI

#### Step 4: Migrate UI Screens
- [ ] Move `HoomeScreen.kt` composable to shared
- [ ] Move `SettingsScreen.kt` composable to shared
- [ ] Move `PdfReaderScreen.kt` composable to shared
- [ ] Move `Fb2ReaderScreen.kt` composable to shared
- [ ] Update imports and replace ViewModel with Koin injections
- [ ] Replace Android Toast with platform-abstracted notifications
- [ ] Replace Android-specific file handling

#### Step 5: Implement Multiplatform Navigation
- [ ] Create `Navigation.kt` with sealed route classes
- [ ] Build route-based state machine
- [ ] Handle back navigation and deep linking
- [ ] Create `MainNavigationScreen` composable

#### Step 6: Update Koin Configuration
- [ ] Expand `shared/src/commonMain/di/KoinModules.kt`
- [ ] Register all state managers as singletons/factories
- [ ] Register platform services
- [ ] Create `shared/src/androidMain/di/AndroidModule.kt` for Android Context
- [ ] Update `ReaderApp.kt` to use new modules

#### Step 7: Update Android App
- [ ] Remove ViewModels from Android module
- [ ] Update `MainActivity.kt` to use shared `MainNavigationScreen`
- [ ] Update `ReaderApp.kt` to initialize only new Koin modules
- [ ] Test Android app

#### Step 8: Create Desktop App UI
- [ ] Create `desktopApp/src/jvmMain/kotlin/.../Main.kt`
- [ ] Initialize `MainNavigationScreen`
- [ ] Set up Koin with desktop modules
- [ ] Implement desktop window management
- [ ] Test desktop app

### Key Implementation Details

#### State Management Pattern (Koin-based)

```kotlin
// Old Android pattern (ViewModel):
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()
}

// New shared pattern (Koin-managed):
class MyScreenState(
    private val repository: MyRepository
) {
    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()
    
    fun onAction(action: MyAction) { /* ... */ }
}

// Usage in composables:
val state: MyScreenState = inject()
```

#### Platform Service Abstraction

```kotlin
// Common interface:
expect interface IPlatformServices {
    fun showToast(message: String)
    fun showDialog(title: String, message: String)
    fun pickFile(): String?
}

// Android implementation:
actual class AndroidPlatformServices(private val context: Context) : IPlatformServices {
    actual fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

// Desktop implementation:
actual class DesktopPlatformServices : IPlatformServices {
    actual fun showToast(message: String) {
        println(message)  // or show Swing notification
    }
}
```

#### Navigation Pattern

```kotlin
sealed class NavigationRoute {
    object Home : NavigationRoute()
    object Settings : NavigationRoute()
    data class PdfReader(val bookId: String) : NavigationRoute()
}

// State machine:
val currentRoute = MutableStateFlow<NavigationRoute>(NavigationRoute.Home)

fun navigateTo(route: NavigationRoute) {
    currentRoute.value = route
}
```

### Dependencies to Add
- Already have: Koin, Compose Multiplatform, Coroutines
- Need to verify: Navigation Compose for multiplatform

### Rollback Plan
- Keep Android ViewModels until full migration is complete
- Test each screen individually before removing Android code
- Maintain git branches for each step

### Success Criteria
1. ✅ All state logic moved to shared module
2. ✅ UI composables work on both Android and Desktop
3. ✅ Navigation works on both platforms
4. ✅ Platform services abstracted properly
5. ✅ Android app still works with new architecture
6. ✅ Desktop app launches and displays main screen
7. ✅ No remaining Android-specific code in commonMain
8. ✅ All Koin injections working

### Timeline Estimate
- State migration: 1-2 days
- UI screen migration: 2-3 days
- Navigation setup: 1 day
- Platform service abstraction: 1 day
- Android/Desktop testing & fixes: 1-2 days

**Total: 6-9 days**

---
## Progress Log

### Session 1: Planning & Analysis
- [x] Analyzed current Android architecture
- [x] Identified 5 ViewModels and their dependencies
- [x] Mapped existing Koin configuration
- [x] Created comprehensive implementation plan

### Next Steps
- [ ] Start with state management migration
- [ ] Create state classes in shared module
- [ ] Convert HoomeScreenViewModel first (most complex)


# Migration Checklist & Timeline

## Phase 1: Foundation ✅ COMPLETED

### Infrastructure Setup
- [x] Update dependency versions (Kotlin 2.0, Compose Multiplatform 1.7.1, etc.)
- [x] Create `shared/` module with KMP configuration
- [x] Create `desktopApp/` module for desktop application
- [x] Update `settings.gradle.kts` with new modules
- [x] Configure multiplatform plugins in root `build.gradle.kts`

### Platform Abstractions
- [x] Create `FileProvider` abstraction (file I/O)
  - [x] Android implementation (Context-based)
  - [x] Desktop implementation (System properties)
- [x] Create `PreferencesStore` abstraction (settings)
  - [x] Android implementation (SharedPreferences)
  - [x] Desktop implementation (Java Preferences)
- [x] Create `BitmapProvider` abstraction (image operations)
  - [x] Android implementation (Bitmap)
  - [x] Desktop implementation (BufferedImage)

### Domain Models
- [x] Create common domain models in `commonMain/`
  - [x] `FormatType` enum (PDF, DJVU, FB2)
  - [x] `FileType` enum
  - [x] `BookDomainModel` sealed class
  - [x] Specific book models (PDF, DjVu, FB2)
  - [x] `BookMetaInfo` for metadata
  - [x] `Progress` sealed class hierarchy

### Book Processing Factories
- [x] Create `IBookCreateFactory` interface
  - [x] Android implementation
  - [x] Desktop implementation
- [x] Create `IPreviewGenerator` interface
  - [x] Android implementation
  - [x] Desktop implementation (PDF only for now)
- [x] Create `IBookInfoExtractor` interface
  - [x] Android implementation
  - [x] Desktop implementation

### Dependency Injection
- [x] Set up Koin for multiplatform
- [x] Create common module in `commonMain/`
- [x] Define all factories with qualifiers

### Documentation
- [x] Create `MULTIPLATFORM_MIGRATION.md` (strategy & structure)
- [x] Create `IMPLEMENTATION_GUIDE.md` (step-by-step)
- [x] Create `README_MULTIPLATFORM.md` (overview)
- [x] Create `quick-start.sh` (build automation)

**Estimated Time: 1-2 weeks** ✅

---

## Phase 2: Business Logic Migration ⏳ IN PROGRESS

### Step 1: Storage & Repository Layer (Week 3)
**Files to move/refactor:**
- [ ] `BookStorage.kt` → Move to shared, use FileProvider
- [ ] `BookStorageRepository.kt` → Move to shared
- [ ] `StorageAdapter.kt` → Move to shared, use FileProvider/PreferencesStore
- [ ] Remove dependencies on Android Context

**Koin bindings:**
- [ ] Add BookStorage to commonModule
- [ ] Add BookStorageRepository to commonModule

**Tests:**
- [ ] Create `commonTest/BookStorageTest.kt`
- [ ] Create `commonTest/RepositoryTest.kt`

### Step 2: Settings Manager (Week 3-4)
**Files to create/refactor:**
- [ ] Create `SettingsManager.kt` using PreferencesStore
  - [ ] Replace SharedPreferences usage
  - [ ] Use suspendable functions (coroutines)
- [ ] Add to commonModule Koin

**Tests:**
- [ ] Create `commonTest/SettingsManagerTest.kt`

### Step 3: Database Migration (Week 4-5)
**Option A: Simple JSON Serialization (Faster, Good for Phase 2)**
- [ ] Create `BookEntity.kt` using @Serializable
- [ ] Create simple file-based persistence
- [ ] Migrate from Room gradually

**Option B: SQLDelight (More Robust, Recommended)**
- [ ] Create `.sq` files for book storage
- [ ] Generate SQLDelight code
- [ ] Implement database access layer
- [ ] Add SQLDelight to `shared/build.gradle.kts`

**Files to migrate:**
- [ ] `AppDatabase.kt` interface → SQL/JSON schema
- [ ] `UserDao.kt` → Repository pattern or SQL queries

### Step 4: Update Android Module (Week 5)
**Changes in `bottomapp/`:**
- [ ] Remove duplicate code (now in shared)
- [ ] Update `MainActivity.kt`:
  - [ ] Remove Koin setup (use shared)
  - [ ] Keep Android-specific lifecycle
- [ ] Update `ReaderApp.kt`:
  - [ ] Initialize shared commonModule()
  - [ ] Add Android-specific bindings only
  - [ ] Keep `PDFBoxResourceLoader.init()`

**Verification:**
- [ ] Android app still builds
- [ ] Android app still runs
- [ ] No compilation errors

**Estimated Time: 2-3 weeks**

---

## Phase 3: UI Component Migration ⏳ NOT STARTED

### Step 1: Extract Common Composables (Week 6-7)
**Move to `shared/src/commonMain/ui/`:**
- [ ] `BookListScreen.kt`
- [ ] `MainScreen.kt`
- [ ] Common UI components (cards, buttons, dialogs)
- [ ] Navigation setup

**Keep platform-specific:**
- [ ] Android Activities/Fragments if needed
- [ ] Desktop window management

### Step 2: Replace Navigation (Week 7-8)
- [ ] Replace Android Navigation with Compose Navigation
- [ ] Set up common navigation graph
- [ ] Test on both Android and Desktop

### Step 3: ViewModel Migration (Week 8)
**Current approach in Android:**
- Uses `androidx.lifecycle.ViewModel`

**Multiplatform approach:**
- [ ] Create KMP-compatible ViewModels in shared
- [ ] Use Koin for ViewModel creation
- [ ] Test on both platforms

### Step 4: UI Polish (Week 8-9)
- [ ] Ensure Material Design 3 consistency
- [ ] Test on various screen sizes
- [ ] Responsive layout for desktop

**Estimated Time: 3-4 weeks**

---

## Phase 4: Desktop App Implementation ⏳ NOT STARTED

### Step 1: File Management (Week 10)
- [ ] Create file browser UI
- [ ] File open dialog
- [ ] File save dialog
- [ ] Recent files list

### Step 2: Reader Screens (Week 10-11)
- [ ] PDF reader screen
- [ ] DjVu reader screen (basic, PDF fallback initially)
- [ ] FB2 reader screen

### Step 3: Desktop-Specific Features (Week 11)
- [ ] Window management (maximize, minimize, resize)
- [ ] Menu bar (File, Edit, View, Help)
- [ ] Keyboard shortcuts
- [ ] System integration (drag & drop files)

### Step 4: Settings & Preferences (Week 11-12)
- [ ] Settings UI for desktop
- [ ] Theme selection
- [ ] Font preferences
- [ ] Reader preferences

**Estimated Time: 2-3 weeks**

---

## Phase 5: Testing & Optimization ⏳ NOT STARTED

### Step 1: Unit Tests (Week 12-13)
- [ ] Test book creation from files
- [ ] Test metadata extraction
- [ ] Test settings storage
- [ ] Test preview generation

### Step 2: Integration Tests (Week 13)
- [ ] Android instrumented tests
- [ ] Desktop JVM tests
- [ ] File I/O tests

### Step 3: UI Tests (Week 13-14)
- [ ] Test book list loading
- [ ] Test reader screens
- [ ] Test navigation

### Step 4: Platform Testing (Week 14)
- [ ] Test on actual Android devices
- [ ] Test on Windows 10/11
- [ ] Test on Ubuntu Linux
- [ ] Test on macOS Ventura+

### Step 5: Performance (Week 14-15)
- [ ] Profile PDF rendering
- [ ] Optimize memory usage
- [ ] Test with large files (100+ MB)
- [ ] Measure startup time

**Estimated Time: 3-4 weeks**

---

## Phase 6: Distribution & Packaging ⏳ NOT STARTED

### Step 1: Android Distribution (Week 15)
- [ ] Update version codes
- [ ] Generate signed APK
- [ ] Prepare for Play Store

### Step 2: Desktop Distribution (Week 15-16)
- [ ] Windows MSI packaging
- [ ] Linux DEB packaging
- [ ] macOS DMG packaging
- [ ] Code signing (if needed)

### Step 3: Release Automation (Week 16)
- [ ] Set up CI/CD (GitHub Actions or similar)
- [ ] Automate builds for all platforms
- [ ] Automate packaging

**Estimated Time: 1-2 weeks**

---

## Timeline Summary

| Phase | Tasks | Timeline | Status |
|-------|-------|----------|--------|
| 1 | Foundation & Abstractions | Week 1-2 | ✅ DONE |
| 2 | Business Logic | Week 3-5 | 🔄 IN PROGRESS |
| 3 | UI Components | Week 6-9 | ⏳ TODO |
| 4 | Desktop App | Week 10-11 | ⏳ TODO |
| 5 | Testing & Optimization | Week 12-15 | ⏳ TODO |
| 6 | Distribution | Week 15-16 | ⏳ TODO |

**Total: 16 weeks (~4 months)** for full multiplatform support

---

## Current Work Items (Next Sprint)

### High Priority
- [ ] Move `BookStorage` to shared module
- [ ] Move `BookStorageRepository` to shared module
- [ ] Create `SettingsManager` using `PreferencesStore`
- [ ] Set up database (choose SQLDelight or JSON)
- [ ] Update Android module to use shared code

### Medium Priority
- [ ] Create first common Composable
- [ ] Set up desktop window management
- [ ] Create file picker for desktop

### Low Priority
- [ ] Add DjVu support to desktop (can use PDF fallback initially)
- [ ] Advanced PDF features
- [ ] UI refinements

---

## Testing Checklist (Before Each Release)

### Functional Tests
- [ ] Can open PDF files
- [ ] Can navigate pages
- [ ] Can zoom in/out
- [ ] Can search text (when implemented)
- [ ] Settings are saved and persisted
- [ ] Recent files list works

### Platform-Specific Tests
- [ ] Android: Orientation change doesn't lose progress
- [ ] Android: App works in split-screen
- [ ] Desktop: Window resize works correctly
- [ ] Desktop: Keyboard shortcuts work
- [ ] Desktop: Drag-and-drop files works

### Performance Tests
- [ ] Startup time < 3 seconds
- [ ] Page navigation < 500ms
- [ ] PDF rendering smooth with zoom
- [ ] Memory usage under 500MB for typical documents

---

## Success Criteria

- ✅ Phase 1: All abstractions implemented and tested
- ✅ Phase 2: 80% of business logic in shared module
- ⏳ Phase 3: UI runs on both Android and Desktop
- ⏳ Phase 4: Desktop app feature-complete
- ⏳ Phase 5: All tests passing on all platforms
- ⏳ Phase 6: Distributable packages for Windows/Linux/macOS

---

## Notes

- **DjVu on Desktop**: Initially use PDF fallback, implement proper DjVu support later
- **Database**: Consider starting with JSON serialization, migrate to SQLDelight in Phase 3-4
- **Testing**: Automate tests early to catch multiplatform issues
- **Documentation**: Update docs as you go, not after
- **Commits**: Make small, focused commits per task
- **Branches**: Use feature branches for each phase

---

## Contact & Support

- See `IMPLEMENTATION_GUIDE.md` for detailed steps
- See `MULTIPLATFORM_MIGRATION.md` for architecture decisions
- See `README_MULTIPLATFORM.md` for quick reference

Last Updated: Dec 2, 2025


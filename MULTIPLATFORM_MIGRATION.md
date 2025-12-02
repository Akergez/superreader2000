# Compose Multiplatform Migration Guide

## Overview
This document outlines the migration of your PDF/e-book reader from Android-only to Compose Multiplatform, supporting Windows, Linux, and macOS.

## Project Structure

```
app/
├── shared/                          # Multiplatform shared code
│   ├── src/commonMain/              # Common code (business logic, UI interfaces)
│   ├── src/androidMain/             # Android-specific implementations
│   ├── src/desktopMain/             # Desktop-specific implementations
│   └── build.gradle.kts
├── bottomapp/                       # Android application
│   ├── src/main/...                 # Existing Android code
│   └── build.gradle.kts             # Updated to depend on :shared
├── desktopApp/                      # Desktop application
│   ├── src/jvmMain/...              # Desktop code
│   └── build.gradle.kts             # Compose Desktop configuration
├── settings.gradle.kts              # Updated with new modules
└── gradle/
    └── libs.versions.toml           # Updated with multiplatform dependencies
```

## Migration Steps

### Phase 1: Foundation Setup (Completed) ✅

1. **Updated dependency versions** in `gradle/libs.versions.toml`
   - Added `compose-multiplatform = "1.7.1"`
   - Added `pdfbox = "3.0.2"` (JVM version)
   - Added `koin-version = "4.0.0"` (multiplatform support)
   - Added `sqldelight-version = "2.1.0"` (cross-platform database)
   - Added `kotlinx-serialization = "1.6.0"`

2. **Created shared module** (`shared/build.gradle.kts`)
   - Configured Kotlin Multiplatform with `jvm("desktop")` and `androidTarget()`
   - Set up source set structure for `commonMain`, `androidMain`, `desktopMain`
   - Added platform-specific dependencies

3. **Created platform abstraction layer** (`shared/src/commonMain/`)
   - `PlatformAbstraction.kt`: Defines expect classes for cross-platform file I/O
   - Created Android implementations in `shared/src/androidMain/`
   - Created Desktop implementations in `shared/src/desktopMain/`

4. **Created preview generator abstraction**
   - Common interface: `IPreviewGenerator`
   - Factory pattern: `PreviewGeneratorFactory`
   - Platform-specific implementations using expect/actual

5. **Created desktop app module** (`desktopApp/`)
   - Configured Compose Desktop with native packaging (DMG, MSI, DEB)
   - Basic entry point in `Main.kt`

### Phase 2: Move Business Logic to Shared (In Progress)

**Tasks:**
1. Move domain models to `shared/src/commonMain/`:
   - `BookDomainModel.kt`
   - `PdfBookDomainModel.kt`
   - `DjvuBookDomainModel.kt`
   - `Fb2BookDomainModel.kt`
   - `FormatType.kt`
   - `FileType.kt`

2. Move and refactor storage layer:
   - `BookManager.kt`
   - `BookStorageRepository.kt`
   - `StorageAdapter.kt` (with platform abstraction)
   - Replace Room with SQLDelight

3. Move and refactor settings:
   - `SettingsManager.kt` using `PreferencesStore`

4. Move book creators:
   - `IBookCreateFactory.kt`
   - `PdfCreateFactory.kt`
   - `DjvuCreateFactory.kt`
   - `Fb2CreateFactory.kt`

5. Move book info extractors:
   - `IBookInfoExtractor.kt`
   - `PdfBookAuthorExtractor.kt`
   - `Fb2BookAuthorExtractor.kt`

### Phase 3: Migrate UI to Shared (Next)

**Tasks:**
1. Move UI composables to `shared/src/commonMain/ui/`
2. Replace Android-specific navigation with Compose Navigation
3. Replace Android ViewModel with Koin-based state management
4. Update all UI screens to work on both platforms

### Phase 4: Update Android App Module

**Tasks:**
1. Remove duplicate code already in shared module
2. Update `MainActivity.kt` to initialize shared Koin modules
3. Update `ReaderApp.kt` to use shared Koin configuration
4. Remove or adapt platform-specific libraries

### Phase 5: Implement Desktop App

**Tasks:**
1. Create desktop main UI in `desktopApp/src/jvmMain/`
2. Set up desktop Koin modules
3. Handle desktop-specific features (window management, file dialogs)
4. Configure packaging for Windows/Linux/macOS

### Phase 6: Testing & Optimization

**Tasks:**
1. Unit tests for shared business logic
2. UI tests for common composables
3. Platform-specific integration tests
4. Performance optimization

## Platform-Specific Code Examples

### File I/O with Platform Abstraction

**Common code** (`shared/src/commonMain/`):
```kotlin
val fileProvider = inject<FileProvider>()
val data = fileProvider.loadFileBytes("path/to/file")
```

**Android implementation** (`shared/src/androidMain/`):
```kotlin
actual class FileProvider(private val context: Context) {
    actual suspend fun loadFileBytes(path: String): ByteArray =
        withContext(Dispatchers.IO) { File(path).readBytes() }
}
```

**Desktop implementation** (`shared/src/desktopMain/`):
```kotlin
actual class FileProvider {
    actual suspend fun loadFileBytes(path: String): ByteArray =
        withContext(Dispatchers.IO) { File(path).readBytes() }
}
```

## Key Technologies

### Compose Multiplatform
- Unified UI framework for Android, Desktop
- Material Design 3 support
- Native performance

### Koin Dependency Injection
- Multiplatform support (4.0+)
- Module-based configuration
- ViewModelFactory support

### SQLDelight
- Cross-platform database
- Type-safe SQL
- Replaces Room

### PDFBox
- Apache PDFBox for JVM (desktop)
- tom-roush PDFBox Android (Android)
- Cross-platform PDF rendering

### Kotlinx Serialization
- Multiplatform serialization
- Replaces Gson
- JSON support out of the box

## Expected Timeline

- **Phase 1-2**: 2-3 weeks (Foundation + Business Logic)
- **Phase 3**: 2-3 weeks (UI Migration)
- **Phase 4-5**: 2 weeks (Platform Apps)
- **Phase 6**: 1-2 weeks (Testing + Optimization)

**Total: 7-11 weeks for full multiplatform support**

## Common Pitfalls to Avoid

1. **Don't put Android-specific code in commonMain**
   - Use expect/actual for platform differences
   - Keep Android imports in androidMain only

2. **Don't forget platform-specific Koin bindings**
   - Desktop needs separate module setup
   - Use qualifier names for platform-specific implementations

3. **Watch out for JVM vs Android differences**
   - File paths differ (Context on Android vs System properties on Desktop)
   - Bitmap implementations differ (Android Bitmap vs AWT BufferedImage)

4. **Test on actual platforms**
   - Desktop requires JVM 11+
   - Verify native packaging works on target platforms

## Building & Running

### Build Shared Module
```bash
./gradlew shared:build
```

### Build Android App
```bash
./gradlew bottomapp:build
./gradlew bottomapp:installDebug
```

### Build Desktop App
```bash
# Run on desktop
./gradlew desktopApp:run

# Create distribution packages
./gradlew desktopApp:packageMsi      # Windows
./gradlew desktopApp:packageDeb      # Linux
./gradlew desktopApp:packageDmg      # macOS
```

## Next Steps

1. Create migration branches for each phase
2. Move domain models to shared
3. Refactor storage layer to use SQLDelight
4. Begin UI component extraction
5. Test on actual Windows/Linux/macOS machines
6. Set up CI/CD for multiplatform builds

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-getting-started.html)
- [Koin Multiplatform](https://insert-koin.io/docs/reference/koin-core/modules)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)


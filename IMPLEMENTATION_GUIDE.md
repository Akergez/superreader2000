# Compose Multiplatform Implementation Guide

## What Has Been Set Up

### 1. Project Structure
```
shared/
├── src/
│   ├── commonMain/          # Shared code for all platforms
│   ├── androidMain/         # Android-specific implementations
│   └── desktopMain/         # Desktop-specific implementations
desktopApp/                  # Standalone desktop application
bottomapp/                   # Android application (updated)
```

### 2. Common Domain Models (Completed)
- `FormatType` enum (PDF, DJVU, FB2)
- `FileType` enum
- `BookDomainModel` sealed class hierarchy
- `PdfBookDomainModel`, `DjvuBookDomainModel`, `Fb2BookDomainModel`
- `BookMetaInfo` for metadata
- `Progress` sealed class with PDF/DjVu/FB2 variants

### 3. Platform Abstraction Layer (Completed)

#### File I/O (`PlatformAbstraction.kt`)
- `FileProvider` - Load/save files, get cache/documents paths
- `PreferencesStore` - Key-value storage
- `BitmapProvider` - Create and convert bitmaps
- `PlatformBitmap` - Platform-specific bitmap representation

**Android Implementation:**
- Uses `Context` for file paths
- Uses `SharedPreferences` for settings
- Uses Android `Bitmap` class

**Desktop Implementation:**
- Uses system home directory (`.pdfreaderkmp/`)
- Uses Java `Preferences` for settings
- Uses AWT `BufferedImage`

### 4. Book Processing Abstraction

#### Preview Generators
- `IPreviewGenerator` - Common interface
- `PreviewGeneratorFactory` - Platform-specific factory
- Implementations for PDF, DjVu, FB2

#### Book Creators
- `IBookCreateFactory` - Factory for creating book objects
- `BookCreatorFactory` - Platform-specific factory
- Loads and parses PDF, DjVu, FB2 files

#### Book Extractors
- `IBookInfoExtractor<T>` - Extract metadata
- `BookExtractorFactory` - Platform-specific factory
- Extract author, title, page count, etc.

### 5. Dependency Injection (Koin)
- `commonModule()` - Shared Koin setup
- Binds all factories and services
- Uses qualifiers for format-specific implementations

## How to Continue the Migration

### Phase 2: Move More Business Logic

#### Step 1: Create Storage Layer
```kotlin
// shared/src/commonMain/kotlin/com/example/bottomapp/shared/backend/storage/
// - BookStorage.kt (main storage manager)
// - BookStorageRepository.kt (repository pattern)
// - StorageAdapter.kt (adapts to platform abstraction)
```

**Key changes from Android code:**
- Replace `Context` with `FileProvider` and `PreferencesStore`
- Replace `Room` with SQLDelight or simple JSON serialization
- Use `expect/actual` for any Android-specific APIs

#### Step 2: Create Settings Manager
```kotlin
// shared/src/commonMain/kotlin/com/example/bottomapp/shared/backend/
// - SettingsManager.kt
```

Use `PreferencesStore` instead of Android `SharedPreferences`

#### Step 3: Migrate Database
Replace Room with SQLDelight:
1. Create `.sq` files in `shared/src/commonMain/sqldelight/`
2. Add SQLDelight plugin to `shared/build.gradle.kts`
3. Generate database code

### Phase 3: Move UI Components

#### Step 1: Analyze Current UI
- Extract common UI composables to `shared/src/commonMain/ui/`
- Keep platform-specific UI in `androidMain/ui/` or `desktopMain/ui/`
- Use Compose Navigation for screen management

#### Step 2: Create Common Composables
```kotlin
// shared/src/commonMain/kotlin/com/example/bottomapp/shared/ui/
// - BookListScreen.kt
// - PdfReaderScreen.kt
// - DjvuReaderScreen.kt
// - Fb2ReaderScreen.kt
// - SettingsScreen.kt
```

#### Step 3: Handle Platform Differences
- File pickers (use different APIs per platform)
- Window management (Android: Activity, Desktop: Window)
- Navigation (use Koin + Compose Navigation)

### Phase 4: Update Android App

In `bottomapp/build.gradle.kts`, already added:
```kotlin
implementation(project(":shared"))
```

#### Required changes in `MainActivity.kt`:
```kotlin
// Remove Android-specific imports from shared code
// Use Koin modules from shared
// Initialize Koin with shared + Android-specific modules
```

#### Required changes in `ReaderApp.kt`:
```kotlin
// Use commonModule() from shared
// Add only Android-specific bindings
// Initialize PDFBoxResourceLoader (Android-specific)
```

### Phase 5: Implement Desktop App

In `desktopApp/src/jvmMain/kotlin/com/example/bottomapp/desktop/`:

```kotlin
fun main() = application {
    // Initialize Koin with commonModule() + desktop modules
    // Create main window with Compose
    // Set up file dialogs (Desktop-specific)
    // Handle window lifecycle
}
```

## Testing Strategy

### Unit Tests
```kotlin
// shared/src/commonTest/
// Test business logic independently of UI
- BookManagerTest.kt
- SettingsManagerTest.kt
- BookCreatorFactoryTest.kt
```

### Integration Tests
```kotlin
// shared/src/androidTest/
// Android-specific integration tests

// shared/src/desktopTest/
// Desktop-specific integration tests
```

### Platform-Specific Tests
```kotlin
// Test platform abstractions
- FileProviderTest (Android vs Desktop)
- PreferencesStoreTest (Android vs Desktop)
```

## Building & Running

### Build Everything
```bash
./gradlew build
```

### Build Individual Modules
```bash
# Build shared module
./gradlew shared:build

# Build Android app
./gradlew bottomapp:build

# Build desktop app
./gradlew desktopApp:build
```

### Run Locally
```bash
# Run desktop app
./gradlew desktopApp:run

# Install Android app (emulator or device must be running)
./gradlew bottomapp:installDebug
```

### Package for Distribution
```bash
# Package desktop for all platforms
./gradlew desktopApp:packageMsi      # Windows installer
./gradlew desktopApp:packageDeb      # Linux package
./gradlew desktopApp:packageDmg      # macOS package

# Or use packageDistributionForCurrentOS
./gradlew desktopApp:packageDistributionForCurrentOS
```

## Common Issues & Solutions

### Issue: Imports of Android classes in shared code
**Solution:** Move to `androidMain/`, use `expect/actual` pattern

### Issue: DjVu rendering not working on desktop
**Solution:** 
- Option 1: Use JNI bindings (complex)
- Option 2: Convert DjVu to PDF on desktop
- Option 3: Start with PDF-only on desktop, add DjVu later

### Issue: PDF rendering differences between platforms
**Solution:** Test thoroughly on both Android and Desktop, adjust rendering parameters if needed

### Issue: File paths differ between platforms
**Solution:** Use `FileProvider.getAppDocumentsPath()` and `FileProvider.getAppCachePath()`, never hardcode paths

### Issue: SQLDelight migration issues
**Solution:** Start with simple JSON serialization first, migrate to SQLDelight incrementally

## Next Steps Checklist

- [ ] Move `BookManager` and `BookStorageRepository` to shared
- [ ] Implement `SettingsManager` using `PreferencesStore`
- [ ] Set up SQLDelight (or JSON serialization) for persistence
- [ ] Extract UI composables to `commonMain`
- [ ] Create desktop-specific file dialogs
- [ ] Update Android `MainActivity` to use shared Koin modules
- [ ] Implement desktop main screen with file browser
- [ ] Test book loading on both platforms
- [ ] Test file I/O on both platforms
- [ ] Add platform-specific packaging configurations
- [ ] Create CI/CD pipeline for multiplatform builds

## Resources for Each Component

### File I/O
- [Kotlin Multiplatform File IO](https://kotlinlang.org/docs/multiplatform.html#expected-and-actual-declarations)
- Java `File` API (works on both platforms)

### Database
- [SQLDelight Official Docs](https://cashapp.github.io/sqldelight/)
- [Room Migration to SQLDelight](https://cashapp.github.io/sqldelight/migrations/)

### PDF Processing
- [Apache PDFBox](https://pdfbox.apache.org/)
- [PDFBox Android](https://github.com/TomRoush/PdfBox-Android)

### DjVu
- [DjVu Specification](http://djvu.org/)
- [GitHub DjVuLibre Android](https://github.com/axet/djvulibre-android)

### Compose Multiplatform
- [Compose Multiplatform Docs](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-getting-started.html)
- [Compose Navigation](https://github.com/adrianwitaszak/navigation-compose-multiplatform)

### Koin
- [Koin Multiplatform Setup](https://insert-koin.io/docs/reference/koin-core/modules)
- [Koin with ViewModels](https://insert-koin.io/docs/reference/koin-compose/viewmodel)

## Support for Additional Platforms

Once Windows/Linux/macOS are working, you can add:
- **iOS**: Requires additional Kotlin Multiplatform setup and different Compose UI
- **Web**: Use Compose for Web (Kotlin/JS)

For now, focus on desktop platforms which share the JVM runtime.


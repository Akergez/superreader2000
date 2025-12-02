# Visual Migration Guide

## Project Architecture After Migration

```
┌─────────────────────────────────────────────────────────────────┐
│                    Compose Multiplatform UI                     │
│          (Same UI code runs on Android and Desktop)             │
└────────────┬─────────────────────────────┬──────────────────────┘
             │                             │
      ┌──────▼──────────┐        ┌────────▼────────┐
      │  Android App    │        │   Desktop App   │
      │  (bottomapp)    │        │  (desktopApp)   │
      └────────┬────────┘        └────────┬────────┘
               │                          │
               └──────────────┬───────────┘
                              │
            ┌─────────────────▼────────────────────┐
            │    Shared Multiplatform Code        │
            │  (shared module - commonMain)       │
            │                                     │
            │  • Domain Models                    │
            │  • Book Processing                  │
            │  • Business Logic                   │
            │  • Common UI Composables (Phase 3)  │
            │  • Dependency Injection             │
            └──────────────┬──────────────────────┘
                           │
            ┌──────────────▼──────────────┐
            │ Platform Abstraction Layer  │
            │   (expect/actual pattern)   │
            └──────┬──────────────┬───────┘
                   │              │
        ┌──────────▼───┐  ┌───────▼──────────┐
        │   Android    │  │    Desktop       │
        │   (JVM 8)    │  │   (JVM 11+)      │
        │              │  │                  │
        │ • FileIO     │  │ • FileIO         │
        │   (Context)  │  │   (Sys props)    │
        │              │  │                  │
        │ • Prefs      │  │ • Prefs          │
        │   (SharedP)  │  │   (JavaPrefs)    │
        │              │  │                  │
        │ • Bitmaps    │  │ • Bitmaps        │
        │   (Bitmap)   │  │   (BufferedImg)  │
        │              │  │                  │
        │ • PDF Render │  │ • PDF Render     │
        │   (PDFBox)   │  │   (PDFBox)       │
        │              │  │                  │
        │ • DjVu       │  │ • DjVu (TODO)    │
        │   (DjVuLib)  │  │                  │
        └──────────────┘  └──────────────────┘
```

## File Organization

```
app/
├── 📁 shared/                    ← NEW: Multiplatform code
│   ├── src/
│   │   ├── commonMain/
│   │   │   └── kotlin/
│   │   │       └── com/example/bottomapp/shared/
│   │   │           ├── backend/
│   │   │           │   ├── domain/
│   │   │           │   │   └── DomainModels.kt
│   │   │           │   ├── bookCreators/
│   │   │           │   │   └── IBookCreateFactory.kt
│   │   │           │   ├── bookExtractors/
│   │   │           │   │   └── IBookInfoExtractor.kt
│   │   │           │   └── previewGenerators/
│   │   │           │       └── IPreviewGenerator.kt
│   │   │           ├── platform/
│   │   │           │   └── PlatformAbstraction.kt (expect)
│   │   │           └── di/
│   │   │               └── KoinModules.kt
│   │   │
│   │   ├── androidMain/
│   │   │   └── kotlin/
│   │   │       └── com/example/bottomapp/shared/
│   │   │           ├── platform/
│   │   │           │   └── PlatformAbstraction.kt (actual)
│   │   │           ├── backend/
│   │   │           │   ├── bookCreators/
│   │   │           │   │   └── BookCreatorFactory.kt
│   │   │           │   ├── bookExtractors/
│   │   │           │   │   └── BookExtractorFactory.kt
│   │   │           │   └── previewGenerators/
│   │   │           │       └── PreviewGeneratorFactory.kt
│   │   │
│   │   └── desktopMain/
│   │       └── kotlin/
│   │           └── com/example/bottomapp/shared/
│   │               ├── platform/
│   │               │   └── PlatformAbstraction.kt (actual)
│   │               ├── backend/
│   │               │   ├── bookCreators/
│   │               │   │   └── BookCreatorFactory.kt
│   │               │   ├── bookExtractors/
│   │               │   │   └── BookExtractorFactory.kt
│   │               │   └── previewGenerators/
│   │               │       └── PreviewGeneratorFactory.kt
│   │
│   └── build.gradle.kts         ← KMP configuration
│
├── 📁 desktopApp/               ← NEW: Desktop application
│   ├── src/
│   │   └── jvmMain/
│   │       └── kotlin/
│   │           └── com/example/bottomapp/desktop/
│   │               └── Main.kt
│   │
│   └── build.gradle.kts         ← Compose Desktop config
│
├── 📁 bottomapp/                ← UPDATED: Android app
│   ├── src/main/
│   │   ├── java/com/example/bottomapp/
│   │   │   ├── MainActivity.kt
│   │   │   └── ReaderApp.kt
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts         ← Now imports :shared
│
├── 📁 gradle/
│   └── libs.versions.toml       ← UPDATED: New dependencies
│
├── 📄 build.gradle.kts          ← UPDATED: KMP plugins
├── 📄 settings.gradle.kts       ← UPDATED: New modules
│
├── 📚 Documentation (NEW)
│   ├── INDEX.md                 ← Start here!
│   ├── MIGRATION_SUMMARY.md     ← Quick overview
│   ├── README_MULTIPLATFORM.md  ← Full reference
│   ├── IMPLEMENTATION_GUIDE.md  ← Step-by-step
│   ├── MULTIPLATFORM_MIGRATION.md ← Architecture
│   └── CHECKLIST.md             ← Task tracking
│
└── 🔧 Utilities
    └── quick-start.sh           ← Build helper
```

## Code Flow: expect/actual Pattern

### Example: File I/O

```
User Code (shared)
    ↓
expect class FileProvider
    ↓
    ├─→ actual class FileProvider (androidMain)
    │   ├─ Uses Android Context
    │   └─ Implementation specific to Android
    │
    └─→ actual class FileProvider (desktopMain)
        ├─ Uses Java File API
        └─ Implementation specific to Desktop
```

## Dependency Flow

```
┌─────────────────────────────────────┐
│   Application Layer                 │
│  ┌─────────────┐   ┌─────────────┐ │
│  │  MainActivity   │   │   Main.kt (Desktop)    │ │
│  └────────┬────┘   └────────┬────┘ │
└───────────┼──────────────────┼─────┘
            │                  │
            └────┬─────────────┘
                 │
        ┌────────▼────────┐
        │  Koin Module    │
        │ commonModule()  │
        │                 │
        │ ├─ FileProvider │
        │ ├─ Factories    │
        │ └─ Services     │
        └────────┬────────┘
                 │
        ┌────────▼──────────────┐
        │ Platform Abstraction  │
        │                       │
        │ ├─ FileProvider       │
        │ ├─ PreferencesStore   │
        │ ├─ BitmapProvider     │
        │ ├─ BookCreatorFactory │
        │ ├─ PreviewGenerator   │
        │ └─ BookExtractor      │
        └────────┬──────────────┘
                 │
        ┌────────▼──────────────┐
        │ Platform Libraries    │
        │                       │
        │ Android:              │
        │ ├─ PDFBox Android     │
        │ ├─ DjVuLibre         │
        │ └─ SharedPreferences  │
        │                       │
        │ Desktop:              │
        │ ├─ PDFBox JVM        │
        │ ├─ Java Preferences   │
        │ └─ AWT BufferedImage  │
        └───────────────────────┘
```

## Phases Timeline

```
Phase 1: Foundation (Weeks 1-2) ✅ COMPLETE
├── Project structure
├── Platform abstractions
├── Domain models
├── Factories
└── Koin setup

Phase 2: Business Logic (Weeks 3-5) 🔄 NEXT
├── BookStorage → shared
├── SettingsManager
├── Database migration
└── Update Android module

Phase 3: UI Components (Weeks 6-9) ⏳ TODO
├── Extract Composables
├── Replace Navigation
├── ViewModel migration
└── UI polish

Phase 4: Desktop App (Weeks 10-11) ⏳ TODO
├── File browser
├── Reader screens
├── Desktop features
└── Settings

Phase 5: Testing (Weeks 12-15) ⏳ TODO
├── Unit tests
├── Integration tests
├── UI tests
└── Performance

Phase 6: Distribution (Weeks 15-16) ⏳ TODO
├── Android packaging
├── Windows/Linux/macOS
└── CI/CD

Total: ~16 weeks (4 months) for full implementation
```

## Build Commands Reference

```
┌─────────────────────────────────────────────────────────────┐
│              Build Command Structure                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ./gradlew [module]:[task] [options]                        │
│                                                              │
│  Module:  shared | bottomapp | desktopApp | (empty = all)  │
│  Task:    build | run | test | package* | clean            │
│  Options: --refresh-dependencies | --info | --debug        │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│  Examples:                                                  │
│                                                              │
│  Build all:                ./gradlew build                 │
│  Build shared:             ./gradlew shared:build          │
│  Run Android:              ./gradlew bottomapp:run         │
│  Run Desktop:              ./gradlew desktopApp:run        │
│  Package Desktop (Win):    ./gradlew desktopApp:packageMsi │
│  Package Desktop (Linux):  ./gradlew desktopApp:packageDeb │
│  Package Desktop (Mac):    ./gradlew desktopApp:packageDmg │
│  Run tests:                ./gradlew test                  │
│  Clean rebuild:            ./gradlew clean build           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow: Opening a PDF

```
User clicks "Open File" (Desktop)
    ↓
File picker dialog → selects file.pdf
    ↓
BookManager.loadBook(file)
    ↓
BookCreatorFactory.createPdfFactory()
    ↓
PdfCreateFactory.create(file) (platform-specific)
    ├─ Android: Uses PDFBox Android
    └─ Desktop: Uses PDFBox JVM
    ↓
PdfBookDomainModel created with metadata
    ↓
PreviewGenerator.generatePreviewBitmap(file)
    ├─ Creates platform-specific bitmap
    │  ├─ Android: android.graphics.Bitmap
    │  └─ Desktop: java.awt.image.BufferedImage
    ↓
UI updates with book in library
    ↓
User clicks "Read"
    ↓
PdfReaderScreen composable opens
    ↓
PDF rendered using platform-specific libraries
    ├─ Android: PDFBox Android
    └─ Desktop: PDFBox JVM
    ↓
User navigates pages (cross-platform Compose UI)
```

## Key Concepts

### expect/actual Pattern
```kotlin
// commonMain - interface
expect class FileProvider {
    fun loadFile(path: String): ByteArray
}

// androidMain - Android implementation
actual class FileProvider {
    actual fun loadFile(path: String): ByteArray {
        // Android-specific code using Context
    }
}

// desktopMain - Desktop implementation
actual class FileProvider {
    actual fun loadFile(path: String): ByteArray {
        // Desktop-specific code using Java File API
    }
}
```

### Koin Dependency Injection
```kotlin
// commonMain
fun commonModule() = module {
    single { FileProvider() }
    single<IBookCreateFactory>(named(PDF)) { 
        bookCreatorFactory.createPdfFactory() 
    }
}

// Usage in any module
val fileProvider = inject<FileProvider>()
val pdfFactory = get<IBookCreateFactory>(named(PDF))
```

### Platform-Specific UI (when needed)
```kotlin
// Desktop-specific file picker
@Composable
expect fun FilePicker(onFileSelected: (File) -> Unit)

// Android-specific with different implementation
@Composable
actual fun FilePicker(onFileSelected: (File) -> Unit) { /* Android impl */ }

// Desktop-specific with different implementation
@Composable
actual fun FilePicker(onFileSelected: (File) -> Unit) { /* Desktop impl */ }
```

---

This visual guide helps understand how all the pieces fit together.
For detailed implementation, see **IMPLEMENTATION_GUIDE.md**


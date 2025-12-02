# Quick Reference Card

## Cheat Sheet for Compose Multiplatform Migration

### File Locations

```
commonMain/     = Code that runs on ALL platforms
androidMain/    = Android-only code
desktopMain/    = Desktop-only code
```

### expect/actual Pattern

```kotlin
// commonMain
expect class FileProvider { ... }

// androidMain & desktopMain
actual class FileProvider { ... }
```

### Import Rules

```
commonMain imports:
  ✅ Kotlin stdlib, kotlinx
  ❌ android.*, com.tom_roush.pdfbox (Android-specific)

androidMain imports:
  ✅ android.*, com.tom_roush.pdfbox
  ❌ java.nio.file (use FileProvider instead)

desktopMain imports:
  ✅ java.*, org.apache.pdfbox
  ❌ android.*, Context, etc.
```

### Common Tasks

#### Use File I/O in Common Code
```kotlin
val fileProvider = inject<FileProvider>()
val data = fileProvider.loadFileBytes(path)
```

#### Use Settings in Common Code
```kotlin
val prefs = inject<PreferencesStore>()
val value = prefs.getString("key")
```

#### Create Platform-Specific Service
```kotlin
// commonMain
expect fun createService(): MyService

// androidMain
actual fun createService(): MyService = AndroidMyService()

// desktopMain
actual fun createService(): MyService = DesktopMyService()
```

### Build Commands

```bash
# Build all
./gradlew build

# Build module
./gradlew :module:build

# Run app
./gradlew :desktopApp:run

# Test
./gradlew :shared:test

# Package (Desktop)
./gradlew :desktopApp:packageDeb     # Linux
./gradlew :desktopApp:packageMsi     # Windows
./gradlew :desktopApp:packageDmg     # macOS
```

### Debugging

```bash
# Clean rebuild
./gradlew clean build

# Refresh dependencies
./gradlew build --refresh-dependencies

# Show dependency tree
./gradlew :shared:dependencies

# Verbose output
./gradlew build --info
```

### Module Dependencies

```
desktopApp depends on shared
bottomapp depends on shared
shared has no platform dependencies (uses expect/actual)
```

### Platform Differences

| Need | Android | Desktop |
|------|---------|---------|
| File access | Context | File API |
| Settings | SharedPrefs | JavaPrefs |
| Bitmap | android.Bitmap | BufferedImage |
| Main thread | Handler | SwingUtilities |
| PDF render | PDFBox Android | PDFBox JVM |

### Documentation Map

| Question | File |
|----------|------|
| What was done? | MIGRATION_SUMMARY.md |
| How do I build? | README_MULTIPLATFORM.md |
| How do I implement X? | IMPLEMENTATION_GUIDE.md |
| Why did we choose X? | MULTIPLATFORM_MIGRATION.md |
| What's the timeline? | CHECKLIST.md |
| How does architecture work? | VISUAL_GUIDE.md |
| Where do I start? | INDEX.md |

### Testing

```bash
# Common tests
./gradlew :shared:commonTest:test

# Android tests
./gradlew :shared:androidTest:test

# Desktop tests
./gradlew :shared:desktopTest:test

# All tests
./gradlew test
```

### Gradle File Structure

**root build.gradle.kts**
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("org.jetbrains.compose") apply false
}
```

**shared/build.gradle.kts**
```kotlin
kotlin {
    jvm("desktop")
    androidTarget()
    sourceSets { ... }
}

android { ... }
```

**desktopApp/build.gradle.kts**
```kotlin
kotlin {
    jvm { compilations.all { ... } }
    sourceSets { ... }
}

compose.desktop { ... }
```

### Common Errors & Fixes

| Error | Fix |
|-------|-----|
| "Unresolved reference" | Check you're in right source set (commonMain vs androidMain) |
| "Android class in common" | Move to androidMain/ or use expect/actual |
| Build stuck | Run `./gradlew clean build --refresh-dependencies` |
| "No suitable loader" | Missing platform-specific dependency in build.gradle.kts |
| Desktop runs but no UI | Check Main.kt entry point is correct |
| Platform-specific code in common | Use expect/actual pattern |

### Source Set Names

```
commonMain           - Shared code
commonTest          - Shared tests
androidMain         - Android implementation
androidTest         - Android tests
androidInstrumentedTest - Instrumented tests
desktopMain         - Desktop implementation
desktopTest         - Desktop tests
jvmMain             - Alternative name for desktop
jvmTest             - Alternative name for desktop test
```

### Phase Checklist

**Phase 1: ✅ DONE**
- [x] Structure
- [x] Abstractions
- [x] Domain models
- [x] Factories
- [x] Koin

**Phase 2: 🔄 IN PROGRESS**
- [ ] BookStorage
- [ ] SettingsManager
- [ ] Database
- [ ] Android update

**Phase 3-6: ⏳ TODO**
- [ ] UI components
- [ ] Desktop app
- [ ] Testing
- [ ] Distribution

### Platform-Specific Package Names

```
Android:
  com.example.bottomapp

Desktop:
  com.example.bottomapp.desktop

Shared:
  com.example.bottomapp.shared
```

### Key Interfaces in Shared

```
FileProvider              - File I/O
PreferencesStore         - Settings
BitmapProvider           - Images
IBookCreateFactory       - Load books
IPreviewGenerator        - Generate previews
IBookInfoExtractor       - Extract metadata
BookCreatorFactory       - Factory for creators
PreviewGeneratorFactory  - Factory for generators
BookExtractorFactory     - Factory for extractors
```

### Koin Usage

```kotlin
// Inject in common code
val provider = inject<FileProvider>()

// Get with qualifier
val factory = get<IBookCreateFactory>(named(FormatType.PDF))

// Register in module
single { FileProvider() }
single<IBookCreateFactory>(named(PDF)) { PdfFactory() }
```

### Quick Debug Checklist

- [ ] Gradle sync successful?
- [ ] Are imports correct for the source set?
- [ ] Is expect/actual declaration present on both sides?
- [ ] Do platform implementations exist in androidMain/ and desktopMain/?
- [ ] Is the dependency declared in build.gradle.kts?
- [ ] Does the app compile individually before multiplatform?

---

**Pro Tips:**
- Always keep commonMain imports clean
- Use meaningful names for expect/actual pairs
- Test each platform separately before merging
- Update docs as you progress
- Commit small changes frequently
- Use qualifiers for format-specific bindings

**Emergency Commands:**
```bash
# Nuclear option
./gradlew clean

# If stuck
./gradlew --stop && ./gradlew build

# Full diagnostic
./gradlew build -x test --info > build.log
```

---

**Remember:** commonMain = shared code, androidMain/desktopMain = platform-specific!


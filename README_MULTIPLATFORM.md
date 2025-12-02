# PDF Reader KMP - Multiplatform Migration

Convert your Android PDF/e-book reader to run on Windows, Linux, and macOS using Kotlin Multiplatform and Compose Multiplatform.

## Overview

This project is being migrated from a single-platform Android app to a **cross-platform application** using:

- **Kotlin Multiplatform Mobile (KMP)** for code sharing
- **Compose Multiplatform** for unified UI framework
- **Android** for mobile (existing)
- **Desktop (Windows, Linux, macOS)** for new platforms

## Supported Formats

- 📄 **PDF** - Full support
- 📚 **DjVu** - Supported on Android, in progress for desktop
- 📖 **FB2** - Basic support (XML-based e-books)

## Project Structure

```
.
├── shared/                 # Multiplatform shared code
│   ├── src/
│   │   ├── commonMain/     # Common code (business logic, models)
│   │   ├── androidMain/    # Android implementations
│   │   ├── desktopMain/    # Desktop implementations
│   │   ├── commonTest/     # Shared tests
│   │   ├── androidTest/    # Android-specific tests
│   │   └── desktopTest/    # Desktop-specific tests
│   └── build.gradle.kts
│
├── bottomapp/              # Android app
│   ├── src/main/
│   └── build.gradle.kts
│
├── desktopApp/             # Desktop app (new)
│   ├── src/jvmMain/
│   └── build.gradle.kts
│
├── gradle/
│   └── libs.versions.toml  # Dependency versions
│
├── MULTIPLATFORM_MIGRATION.md   # Detailed migration guide
├── IMPLEMENTATION_GUIDE.md       # Implementation details
├── quick-start.sh               # Build script
└── README.md                    # This file
```

## Quick Start

### Prerequisites

- **Java 11+** (for desktop support)
- **Kotlin 2.0+**
- **Gradle 8.0+**
- **Android SDK** (for Android development) - Optional
- **Xcode** (for macOS native packaging) - Optional

### Build All Modules

```bash
# Make script executable
chmod +x quick-start.sh

# Run quick start
./quick-start.sh
```

Or manually:

```bash
# Build everything
./gradlew build

# Build specific modules
./gradlew shared:build           # Shared code
./gradlew bottomapp:build        # Android
./gradlew desktopApp:build       # Desktop
```

### Run Applications

#### Android
```bash
# Build and install
./gradlew bottomapp:installDebug

# Run in emulator or device
./gradlew bottomapp:run
```

#### Desktop
```bash
# Run development version
./gradlew desktopApp:run

# Package for current OS
./gradlew desktopApp:packageDistributionForCurrentOS
```

## Architecture

### Platform Abstraction Layer

The app uses Kotlin's `expect/actual` pattern to abstract platform differences:

```
┌─────────────────────────────────────────┐
│      Common Code (Business Logic)       │
│  ├─ Domain Models                       │
│  ├─ Book Processing                     │
│  ├─ Dependency Injection                │
│  └─ UI Composables                      │
├─────────────────────────────────────────┤
│ Expect/Actual Layer (Platform APIs)     │
├──────────────────┬──────────────────────┤
│  Android         │      Desktop (JVM)   │
│  ─────────────   │      ─────────────   │
│  FileProvider    │      FileProvider    │
│  Preferences     │      Preferences     │
│  Bitmap ops      │      Bitmap ops      │
└──────────────────┴──────────────────────┘
```

### Key Abstractions

#### FileProvider
Cross-platform file I/O operations
- Load/save files
- Cache and documents directories
- Platform-specific implementations

#### PreferencesStore
Key-value storage for app settings
- Android: SharedPreferences
- Desktop: Java Preferences

#### BitmapProvider
Image creation and conversion
- Android: android.graphics.Bitmap
- Desktop: java.awt.image.BufferedImage

#### Factories
Platform-specific implementations injected via Koin
- `BookCreatorFactory` - Load book files
- `PreviewGeneratorFactory` - Generate previews
- `BookExtractorFactory` - Extract metadata

## Building for Distribution

### Windows
```bash
./gradlew desktopApp:packageMsi
# Output: desktopApp/build/compose/binaries/main/msi/
```

### Linux (Ubuntu/Debian)
```bash
./gradlew desktopApp:packageDeb
# Output: desktopApp/build/compose/binaries/main/deb/
```

### macOS
```bash
./gradlew desktopApp:packageDmg
# Output: desktopApp/build/compose/binaries/main/dmg/
```

## Migration Status

### ✅ Completed

- [x] Project structure reorganization
- [x] Kotlin Multiplatform setup
- [x] Compose Multiplatform configuration
- [x] Platform abstraction layer (File I/O, Preferences, Bitmaps)
- [x] Domain models (multiplatform)
- [x] Book creator factories (expect/actual)
- [x] Book extractor factories (expect/actual)
- [x] Preview generator factories (expect/actual)
- [x] Koin dependency injection setup
- [x] Android app module integration
- [x] Desktop app basic structure
- [x] Documentation and guides

### 🔄 In Progress

- [ ] Move UI composables to shared module
- [ ] Migrate database layer (Room → SQLDelight or JSON)
- [ ] Implement desktop file picker UI
- [ ] PDF reader screen (desktop)
- [ ] DjVu support on desktop
- [ ] FB2 reader implementation
- [ ] Testing suite

### ⏳ TODO

- [ ] Performance optimization
- [ ] Advanced PDF features (annotations, search)
- [ ] UI polish and refinement
- [ ] Packaging and distribution setup
- [ ] CI/CD pipeline
- [ ] Platform-specific packaging for Windows/Linux/macOS

## Key Technologies

### Kotlin Multiplatform
- Shared code compilation for Android, JVM (desktop)
- `expect/actual` declarations for platform-specific code
- Common standard library

### Compose Multiplatform
- Unified UI framework based on Jetpack Compose
- Material Design 3 support
- Native performance on all platforms

### PDFBox
- Apache PDFBox for JVM (desktop)
- Tom Roush PDFBox Android
- Cross-platform PDF rendering

### Koin
- Lightweight dependency injection
- Multiplatform support
- Module-based configuration

### Gradle
- KMP plugin configuration
- Multi-module builds
- Platform-specific dependencies

## Development Guide

### Adding a Feature

1. **Implement in common**: `shared/src/commonMain/`
2. **Handle platform differences**: Use `expect/actual` if needed
3. **Add platform-specific code**: `shared/src/androidMain/` or `shared/src/desktopMain/`
4. **Test**: Add tests in `commonTest/`, `androidTest/`, `desktopTest/`
5. **Document**: Update relevant docs

### Adding Platform-Specific Code

```kotlin
// common/Common.kt
expect class MyPlatformClass {
    fun doSomething(): String
}

// androidMain/Android.kt
actual class MyPlatformClass {
    actual fun doSomething(): String = "Android implementation"
}

// desktopMain/Desktop.kt
actual class MyPlatformClass {
    actual fun doSomething(): String = "Desktop implementation"
}
```

### File Organization

- Parallel structure in `commonMain/`, `androidMain/`, `desktopMain/`
- Same package names across all source sets
- Shared interfaces in `commonMain`
- Platform implementations in respective directories

## Troubleshooting

### Build Issues

**Issue**: Gradle sync fails with missing dependencies
```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

**Issue**: Kotlin multiplatform errors
```bash
# Update Kotlin plugin
# In gradle/libs.versions.toml:
kotlin = "2.1.0"
```

### Runtime Issues

**Issue**: File not found on desktop
- Verify using `FileProvider.getAppDocumentsPath()`
- Never hardcode paths

**Issue**: Bitmap rendering differences
- Test on both Android and desktop
- Adjust parameters as needed

**Issue**: DjVu not working
- Use PDF preview instead (temporary)
- DjVu desktop support is in progress

## Performance Considerations

- **Shared code**: ~80% of business logic
- **Platform-specific**: ~20% for UI and native APIs
- **Compilation**: Multiplatform builds take longer
- **Binary size**: Desktop bundles include JVM (50-100 MB base)

## Testing

### Run All Tests
```bash
./gradlew test
```

### Test Specific Module
```bash
./gradlew shared:test
./gradlew bottomapp:test
./gradlew desktopApp:test
```

### Android Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## Contributing

When contributing to the migration:

1. Maintain separation between platform code
2. Use `expect/actual` pattern for platform differences
3. Keep common code in `commonMain`
4. Test on both Android and desktop
5. Update documentation as you go
6. Follow Kotlin style guide

## Resources

### Official Documentation
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-getting-started.html)
- [Gradle Documentation](https://docs.gradle.org/)

### Libraries Used
- [Koin](https://insert-koin.io/docs/reference/koin-compose/)
- [Apache PDFBox](https://pdfbox.apache.org/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

### Related Files
- `MULTIPLATFORM_MIGRATION.md` - Detailed migration strategy
- `IMPLEMENTATION_GUIDE.md` - Step-by-step implementation
- `quick-start.sh` - Automated build script

## License

This project maintains the original license from the Android app.

## Support

For migration-specific issues:
1. Check `MULTIPLATFORM_MIGRATION.md`
2. Review `IMPLEMENTATION_GUIDE.md`
3. Consult the Kotlin Multiplatform official docs
4. Check Compose Multiplatform examples

## Next Steps

1. Run `./quick-start.sh` to build everything
2. Test on both Android and Desktop
3. Review `IMPLEMENTATION_GUIDE.md` for next phases
4. Move UI components to shared module
5. Migrate database to SQLDelight
6. Implement desktop-specific features

---

**Happy coding!** 🚀

For detailed implementation steps, see `IMPLEMENTATION_GUIDE.md`


# Build Instructions for All Platforms

## Overview
This project supports building for:
- **Android** (APK/AAB)
- **Linux** (RPM for x86_64 and arm64)
- **macOS** (DMG for arm64)

---

## Android Build

### Debug APK
```bash
./gradlew :superreader2000:assembleDebug
```
Output: `superreader2000/build/outputs/apk/debug/superreader2000-debug.apk`

### Release APK
```bash
./gradlew :superreader2000:assembleRelease
```
Output: `superreader2000/build/outputs/apk/release/superreader2000-release.apk`

### App Bundle (AAB) for Play Store
```bash
./gradlew :superreader2000:bundleRelease
```
Output: `superreader2000/build/outputs/bundle/release/superreader2000-release.aab`

---

## Desktop Builds

### macOS DMG (arm64 - Apple Silicon)

Build on a macOS arm64 machine:
```bash
./gradlew :desktopApp:packageDmg
```
Output: `desktopApp/build/compose/binaries/main/dmg/PDFReaderKMP-1.0.0.dmg`

### Linux RPM

#### Build for x86_64
Build on a Linux x86_64 machine:
```bash
./gradlew :desktopApp:packageRpm
```
Output: `desktopApp/build/compose/binaries/main/rpm/pdfreaderkmp-1.0.0-1.x86_64.rpm`

#### Build for arm64
Build on a Linux arm64 machine:
```bash
./gradlew :desktopApp:packageRpm
```
Output: `desktopApp/build/compose/binaries/main/rpm/pdfreaderkmp-1.0.0-1.aarch64.rpm`

---

## Build All (Platform Dependent)

### Build everything for current platform:
```bash
./gradlew :superreader2000:assembleRelease :desktopApp:packageDistributionForCurrentOS
```

### Run desktop app (development):
```bash
./gradlew :desktopApp:run
```

---

## Cross-Platform Build Notes

⚠️ **Important**: Native distribution packages can only be built on their target platform:
- **DMG** (macOS) → Must be built on macOS
- **RPM** (Linux) → Must be built on Linux
- **Android** → Can be built on any platform with Android SDK

For CI/CD pipelines, use separate build agents/runners for each target platform:
- GitHub Actions: Use `macos-latest` for DMG, `ubuntu-latest` for RPM
- GitLab CI: Use appropriate runners with `tags: [macos]` or `tags: [linux]`

---

## Build Requirements

### Android
- JDK 17+
- Android SDK (API 36)

### Desktop (macOS/Linux)
- JDK 17+
- For RPM: `rpmbuild` tool (`dnf install rpm-build` on Fedora/RHEL)
- For DMG: Xcode command line tools (`xcode-select --install`)


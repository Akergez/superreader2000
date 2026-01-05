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

### Release APK (unsigned)
```bash
./gradlew :superreader2000:assembleRelease
```
Output: `superreader2000/build/outputs/apk/release/superreader2000-release.apk`

### Signed Release APK

#### Step 1: Generate a Keystore (one-time setup)
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```
- Store the keystore file securely and remember your passwords
- **Never commit the keystore to version control**

#### Step 2: Configure Signing in build.gradle.kts

Add signing config to `superreader2000/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/my-release-key.jks")
            storePassword = "your-store-password"
            keyAlias = "my-key-alias"
            keyPassword = "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...existing config...
        }
    }
}
```

#### Step 2 (Alternative): Use Environment Variables (recommended for CI/CD)
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...existing config...
        }
    }
}
```

Then set environment variables before building:
```bash
export KEYSTORE_PATH=/path/to/my-release-key.jks
export KEYSTORE_PASSWORD=your-store-password
export KEY_ALIAS=my-key-alias
export KEY_PASSWORD=your-key-password
./gradlew :superreader2000:assembleRelease
```

#### Step 3: Build the Signed APK
```bash
./gradlew :superreader2000:assembleRelease
```
Output: `superreader2000/build/outputs/apk/release/superreader2000-release.apk`

#### Manual Signing (without modifying build.gradle.kts)
You can also sign an unsigned APK manually:
```bash
# Build unsigned APK
./gradlew :superreader2000:assembleRelease

# Sign with apksigner (from Android SDK build-tools)
apksigner sign --ks my-release-key.jks --out superreader2000-signed.apk superreader2000/build/outputs/apk/release/superreader2000-release-unsigned.apk

# Verify signature
apksigner verify superreader2000-signed.apk
```

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
Output: `desktopApp/build/compose/binaries/main/dmg/SuperReader2000-1.0.0.dmg`

### Linux RPM

> ⚠️ **RPM packages can ONLY be built on Linux systems.** Running `packageRpm` on macOS or Windows will not produce an RPM file - no error will be shown, but the rpm folder will not be created.

#### Option A: Build on macOS using Docker (Recommended)

Make sure Docker Desktop is installed and running, then:
```bash
docker run --rm -v "$(pwd)":/app -w /app fedora:latest bash -c "
  dnf install -y java-17-openjdk java-17-openjdk-devel rpm-build && 
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && 
  ./gradlew :desktopApp:packageRpm
"
```
Output: `desktopApp/build/compose/binaries/main/rpm/superreader2000-1.0.0-1.x86_64.rpm`

> Note: This builds an x86_64 RPM. For arm64 RPM, use `--platform linux/arm64` flag with Docker.

#### Option B: Build natively on Linux

##### Prerequisites
Install `rpmbuild` tool before building:
```bash
# Fedora/RHEL/CentOS
sudo dnf install rpm-build

# Ubuntu/Debian (if building RPM on Debian-based systems)
sudo apt install rpm
```

##### Build for x86_64
Build on a Linux x86_64 machine:
```bash
./gradlew :desktopApp:packageRpm
```
Output: `desktopApp/build/compose/binaries/main/rpm/superreader2000-1.0.0-1.x86_64.rpm`

##### Build for arm64
Build on a Linux arm64 machine:
```bash
./gradlew :desktopApp:packageRpm
```
Output: `desktopApp/build/compose/binaries/main/rpm/superreader2000-1.0.0-1.aarch64.rpm`

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

---

## GitLab CI/CD Setup

The project includes a `.gitlab-ci.yml` file that automates building for all platforms, publishing to GitLab Package Registry, and creating releases.

### Required CI/CD Variables

Set these variables in **GitLab → Settings → CI/CD → Variables**:

| Variable | Type | Description |
|----------|------|-------------|
| `ANDROID_KEYSTORE_BASE64` | Variable (masked) | Base64-encoded Android keystore file |
| `KEYSTORE_PASSWORD` | Variable (masked) | Keystore password |
| `KEY_ALIAS` | Variable (masked) | Key alias in the keystore |
| `KEY_PASSWORD` | Variable (masked) | Key password |

### Encoding Your Keystore

To encode your keystore for CI/CD:
```bash
base64 -i my-release-key.jks | pbcopy  # macOS (copies to clipboard)
# OR
base64 my-release-key.jks > keystore_base64.txt  # Save to file
```

### Required GitLab Runners

The pipeline requires runners with specific tags:

| Platform | Required Tags | Description |
|----------|---------------|-------------|
| Android | `docker` | Any Docker-capable runner |
| Linux x86_64 | `docker`, `linux`, `amd64` | Linux x86_64 runner |
| Linux arm64 | `docker`, `linux`, `arm64` | Linux ARM64 runner |
| macOS arm64 | `macos`, `arm64` | Apple Silicon Mac runner |
| macOS x86_64 | `macos`, `amd64` | Intel Mac runner |

> **Note**: If you don't have all runner types, some builds will be skipped (`allow_failure: true`).

### Creating a Release

Releases are automatically created when you push a tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```

This will:
1. Build all artifacts (APK, AAB, RPM, DMG)
2. Upload to GitLab Package Registry
3. Create a GitLab Release with download links

### Manual Workflow

For builds on the default branch (without tags), artifacts are still built and uploaded to the Package Registry, but no formal release is created.


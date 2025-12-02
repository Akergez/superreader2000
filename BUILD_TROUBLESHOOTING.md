# Build Troubleshooting Guide

## Current Status

✅ **Gradle configuration files fixed**
- Updated compileSdk to 36 (latest)
- Updated targetSdk to 36
- Removed deprecated composeOptions configuration

⚠️ **System Requirements**
- Java 11+ is required for Compose Multiplatform desktop builds
- Make sure Java is properly installed

---

## Building the Project

### Prerequisites

1. **Install Java** (macOS)
   ```bash
   # If using Homebrew
   brew install openjdk@11
   
   # Or download from: https://www.oracle.com/java/technologies/downloads/
   
   # Verify installation
   java -version
   ```

2. **Set JAVA_HOME** (if needed)
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 11)
   # Add to ~/.zshrc to make it permanent
   ```

3. **Android SDK** (for Android development)
   - Install Android Studio or Android Command Line Tools
   - Set ANDROID_HOME environment variable

### Build Commands

```bash
# Build all modules
./gradlew build

# Build with more info
./gradlew build --info

# Clean and rebuild
./gradlew clean build

# Skip tests (faster)
./gradlew build -x test

# Build specific module
./gradlew shared:build
./gradlew bottomapp:build
./gradlew desktopApp:build
```

### Run Applications

```bash
# Run Desktop app
./gradlew desktopApp:run

# Run Android app
./gradlew bottomapp:installDebug

# Run tests
./gradlew test
```

---

## Common Build Errors & Solutions

### Error: "Unable to locate a Java Runtime"

**Cause:** Java is not installed or not in PATH

**Fix:**
```bash
# Install Java on macOS
brew install openjdk@11

# Add to ~/.zshrc
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

# Apply changes
source ~/.zshrc

# Verify
java -version
```

### Error: "Gradle could not be started"

**Fix:**
```bash
# Clean gradle cache
./gradlew clean

# Refresh dependencies
./gradlew build --refresh-dependencies
```

### Error: "No Android SDK found"

**Fix:**
```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"

# Add to ~/.zshrc permanently
```

### Error: "Kotlin multiplatform compilation failed"

**Fix:**
1. Check you're in the correct source set directory
2. Verify expect/actual classes match
3. Run: `./gradlew clean build`

### Error: "Missing KMP plugin"

**Fix:** The plugins are already configured in `build.gradle.kts`. Run:
```bash
./gradlew clean build --refresh-dependencies
```

---

## Gradle Configuration Files Fixed

### 1. bottomapp/build.gradle.kts
✅ Updated compileSdk from 35 to 36
✅ Updated targetSdk from 35 to 36
✅ Removed deprecated composeOptions block

### 2. shared/build.gradle.kts
✅ Updated compileSdk from 35 to 36
✅ Removed deprecated composeOptions block

### 3. build.gradle.kts (root)
✅ Correct KMP plugin configuration
✅ All required plugins included

### 4. gradle/libs.versions.toml
✅ All multiplatform dependencies included
✅ Correct versions for all libraries

---

## Verification

### Check Gradle Wrapper
```bash
./gradlew --version
```

### Check Java Version
```bash
java -version
```

### Check Android SDK
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --list
```

### Check Project Structure
```bash
./gradlew projects
```

---

## Next Steps

1. **Install Java if not already done:**
   ```bash
   brew install openjdk@11
   ```

2. **Set up environment:**
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 11)
   ```

3. **Build the project:**
   ```bash
   ./gradlew build
   ```

4. **If build succeeds, run:**
   ```bash
   ./gradlew desktopApp:run      # Desktop
   ./gradlew bottomapp:run        # Android (if emulator is running)
   ```

---

## Still Having Issues?

### Get More Diagnostic Info

```bash
# Verbose output
./gradlew build -v

# Full debug info
./gradlew build --debug 2>&1 | tee build.log

# Check dependencies
./gradlew dependencies
```

### Check File Permissions

```bash
# Make gradle wrapper executable
chmod +x ./gradlew

# Make build script executable
chmod +x ./quick-start.sh
```

### Reset Everything

```bash
# Clean
./gradlew clean

# Delete gradle cache
rm -rf ~/.gradle/caches

# Rebuild
./gradlew build --refresh-dependencies
```

---

## Expected Build Output

A successful build should show:
```
BUILD SUCCESSFUL in Xs
```

And create:
- `build/` directories in each module
- `.gradle/` directory with downloaded dependencies
- `desktopApp/build/compose/binaries/` (for desktop packaging)

---

## Support Resources

- [Kotlin Multiplatform Troubleshooting](https://kotlinlang.org/docs/multiplatform-troubleshooting.html)
- [Gradle Troubleshooting](https://docs.gradle.org/current/userguide/troubleshooting.html)
- [Compose Multiplatform Issues](https://github.com/JetBrains/compose-multiplatform/issues)

---

**Version:** 1.0  
**Last Updated:** December 2, 2025

After fixing Java/environment issues, run `./gradlew build` to compile the project.


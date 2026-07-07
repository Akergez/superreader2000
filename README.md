# PDF & FB2 Reader for Android

A simple and modern Android application for reading PDF and FB2 files, built with Material You design principles and Jetpack Compose. The app features synchronization of reading progress in an open file format, making it easy to pick up where you left off across devices.

## Features

- 📄 **PDF Reader**: Open and read PDF files with ease.
- 🎨 **Material You Interface**: Enjoy a clean and customizable UI that adapts to your device's theme.
- 🛠️ **Jetpack Compose**: The app is built using modern Android UI toolkit, Jetpack Compose, for a smooth and responsive experience.
- 🔄 **Progress Sync**: Reading progress will be synchronized via an open file format (in development).

## Roadmap

- [x] Open and read PDF files.
- [x] Add support for FB2 files.
- [x] Implement reading progress saving.
- [x] Synchronize reading progress across devices via an open file format.
- [x] Improve UI for file management and book navigation.
- [ ] Improve FB2 support
    - [ ] Add table of contents support
    - [ ] Improve performance
    - [ ] Use byte offset to sync instead of word offset
- [ ] Freeze sync formats and write docs
- [ ] Write docs for format support creation
- [ ] Create opensource self-hosted ready server and implement built-in sync support

## Dependencies: tresse-kmp & tresse

Progress sync is built on [tresse](https://gitlab.com/ragusseven/tresse) (a Rust CRDT sync engine) via its Kotlin Multiplatform wrapper [tresse-kmp](https://gitlab.com/ragusseven/tresse-kmp). Both live in **separate repositories** and are not published to a Maven repository — `tresse-kmp` is included as a Gradle subproject whose `projectDir` is redirected to an external checkout, and `tresse-kmp` itself does the same for the Rust sources it builds.

### Getting the source

Clone all three repositories as siblings:

```bash
git clone git@gitlab.com:readerproject/app.git app
git clone https://gitlab.com/ragusseven/tresse-kmp.git tresse-kmp
git clone https://gitlab.com/ragusseven/tresse.git tresse
cd app
```

### How the wiring works

- `app/settings.gradle.kts` resolves `tresse-kmp`'s location from the `TRESSE_KMP_DIR` env var, falling back to `../tresse-kmp`, and fails fast with a clear message if it isn't found.
- `tresse-kmp/build.gradle.kts` in turn resolves the Rust sources from `TRESSE_DIR`, falling back to `../tresse`, and shells out to `cargo build --release -p tresse-ffi` to produce the native library used by the desktop target.
- Android doesn't need `tresse`/Rust at build time — the native `.so` files for the Rust FFI are pre-built and checked into `tresse-kmp/src/androidMain/jniLibs/`.

### Building

With the default sibling layout, no extra setup is needed:

```bash
./gradlew :superreader2000:assembleDebug   # Android only — tresse (Rust) not required
./gradlew :desktopApp:packageDmg           # Desktop — requires a Rust toolchain (cargo) in PATH
```

If the repositories live elsewhere, point the build at them explicitly:

```bash
export TRESSE_KMP_DIR=/path/to/tresse-kmp
export TRESSE_DIR=/path/to/tresse   # only needed for desktop targets
./gradlew :superreader2000:assembleDebug
```

CI (`.gitlab-ci.yml`) reproduces this automatically: it `git clone`s `tresse-kmp` (and, for desktop jobs, `tresse` plus a `rustup`-installed toolchain) into `.deps/` and exports `TRESSE_KMP_DIR`/`TRESSE_DIR` before invoking Gradle — see `BUILD_INSTRUCTIONS.md` for the full CI/CD setup.

## Used libs
- [DjvuLibre bindings](https://gitlab.com/axet/android-djvulibre)
- [Zoomable](https://github.com/usuiat/Zoomable)
- [Page Curl](https://github.com/oleksandrbalan/pagecurl)
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

## Used libs
- [DjvuLibre bindings](https://gitlab.com/axet/android-djvulibre)
- [Zoomable](https://github.com/usuiat/Zoomable)
- [Page Curl](https://github.com/oleksandrbalan/pagecurl)
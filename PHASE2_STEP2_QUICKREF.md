# Phase 2 Step 2: Storage Layer - Quick Reference

## What Was Moved

✅ Settings Management
- App theme preferences
- Document display preferences
- Flip animation mode
- Storage directory path

✅ Book Repository
- Book collection management
- Book metadata storage
- Progress tracking
- Book queries

✅ Serialization
- JSON serialization/deserialization
- Metadata file naming conventions

## Key Interfaces

### ISettingsStore
```kotlin
interface ISettingsStore {
    suspend fun getString(key: String, default: String = ""): String
    suspend fun putString(key: String, value: String)
    suspend fun getInt(key: String, default: Int = 0): Int
    suspend fun putInt(key: String, value: Int)
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun removeKey(key: String)
}
```

### ISettingsManager
```kotlin
interface ISettingsManager {
    val appThemeModeFlow: StateFlow<AppThemeMode>
    suspend fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode
    suspend fun setAppTheme(theme: AppThemeMode)
    suspend fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode
    suspend fun setDocumentsTheme(theme: DocThemeMode)
    suspend fun getFlipMode(): FlipMode
    suspend fun setFlipMode(mode: FlipMode)
    suspend fun getStoragePath(): String?
    suspend fun setStoragePath(path: String)
    suspend fun clearStoragePath()
}
```

### IBookRepository
```kotlin
interface IBookRepository {
    suspend fun getAllBooks(): List<BookStorageEntry>
    suspend fun getBookFlow(): Flow<List<BookStorageEntry>>
    suspend fun saveBooks(books: List<BookStorageEntry>)
    suspend fun addBook(book: BookStorageEntry)
    suspend fun updateBook(book: BookStorageEntry)
    suspend fun findBook(bundleName: String): BookStorageEntry?
    suspend fun deleteBook(bundleName: String)
}
```

## Platform Implementations

### Android
- **PreferencesStore**: Uses `SharedPreferences`
- **BookRepository**: In-memory cache (ready for Room integration)
- **Storage Path**: `context.getSharedPreferences("app_settings", ...)`

### Desktop
- **PreferencesStore**: Uses `java.util.Properties`
- **BookRepository**: File-based with in-memory cache
- **Storage Path**: `~/.config/pdfreaderr/prefs.properties`

## Usage Examples

### Settings

```kotlin
// Get settings manager
val settingsManager: ISettingsManager = koinInject()

// Get app theme
val theme = settingsManager.getAppTheme()

// Set app theme
settingsManager.setAppTheme(AppThemeMode.Dark)

// Observe theme changes (reactive)
settingsManager.appThemeModeFlow.collect { newTheme ->
    updateTheme(newTheme)
}

// Get/Set flip mode
settingsManager.setFlipMode(FlipMode.PagingCurl)
val flipMode = settingsManager.getFlipMode()

// Storage path management
settingsManager.setStoragePath("/path/to/books")
val storagePath = settingsManager.getStoragePath()
settingsManager.clearStoragePath()
```

### Book Repository

```kotlin
// Get repository
val bookRepository: IBookRepository = koinInject()

// Get all books
val allBooks = bookRepository.getAllBooks()

// Observe books as flow
bookRepository.getBookFlow().collect { books ->
    updateBookList(books)
}

// Add a new book
val newBook = BookStorageEntry(
    title = "My PDF",
    bundleName = "my-pdf",
    progressInPercent = 0.0,
    bookMetaInfo = BookMetaInfo(
        title = "My PDF",
        author = "Author Name"
    )
)
bookRepository.addBook(newBook)

// Find a book
val book = bookRepository.findBook("my-pdf")

// Update book progress
val updatedBook = book?.copy(progressInPercent = 50.0)
if (updatedBook != null) {
    bookRepository.updateBook(updatedBook)
}

// Delete book
bookRepository.deleteBook("my-pdf")
```

## Koin DI Setup

```kotlin
// In your Koin module
single<PreferencesStore> { 
    PreferencesStore()
}

single<ISettingsManager> { 
    DefaultSettingsManager(get<PreferencesStore>())
}

single<IBookRepository> { 
    BookRepository(get<PreferencesStore>())
}
```

## File Locations

### Android
- Preferences: Application's SharedPreferences (managed by Android)
- Books: Optional integration with Room database

### Desktop
- Preferences: `~/.config/pdfreaderr/prefs.properties`
- Books: `~/.config/pdfreaderr/books/`

## Data Structures

### BookStorageEntry
```kotlin
@Serializable
data class BookStorageEntry(
    val title: String,              // Display title
    val bundleName: String,         // Unique identifier
    val progressInPercent: Double?, // Reading progress 0-100
    val bookMetaInfo: BookMetaInfo  // Book metadata
)
```

### BookMetaInfo
```kotlin
@Serializable
data class BookMetaInfo(
    val author: String = "",
    val title: String = "",
    val subject: String = "",
    val pageCount: Int = 0,
    val producer: String = "",
    val pdfProgress: PdfProgress? = null,
    val djvuProgress: DjvuProgress? = null,
    val fb2Progress: Fb2Progress? = null
)
```

### AppThemeMode
```kotlin
enum class AppThemeMode {
    Light,
    Dark,
    System
}
```

### DocThemeMode
```kotlin
enum class DocThemeMode {
    AsIs,
    Invert
}
```

### FlipMode
```kotlin
enum class FlipMode {
    PagingCurl,
    SlideAnimation,
    DirectFlip
}
```

## Migration Checklist

- [ ] Update Koin DI configuration
- [ ] Replace `SettingsManager` usage with `ISettingsManager`
- [ ] Replace `BookStorageRepository` usage with `IBookRepository`
- [ ] Update BookManager to use new repositories
- [ ] Test on both Android and Desktop
- [ ] Remove old Android-specific storage code

## Common Patterns

### Reactive Theme Updates
```kotlin
LaunchedEffect(Unit) {
    settingsManager.appThemeModeFlow.collect { theme ->
        applyTheme(theme)
    }
}
```

### Safe Book Operations
```kotlin
bookRepository.findBook(bundleName)?.let { book ->
    val updated = book.copy(progressInPercent = newProgress)
    bookRepository.updateBook(updated)
}
```

### Settings with Fallbacks
```kotlin
val theme = runBlocking {
    settingsManager.getAppTheme(AppThemeMode.System)
}
```

## Error Handling

All storage operations are coroutine-based and can throw exceptions:

```kotlin
try {
    val books = bookRepository.getAllBooks()
} catch (e: Exception) {
    Log.e("Storage", "Failed to load books", e)
}
```

## Performance Notes

- Settings are lazily cached
- Book repository maintains in-memory cache
- File I/O happens on `Dispatchers.IO`
- Preferences stored in single file (Android) or properties file (Desktop)
- Books collection grows with number of entries

## Testing

```kotlin
// Mock implementation for testing
class MockSettingsManager : ISettingsManager {
    // Implement with test data
}

class MockBookRepository : IBookRepository {
    // Implement with test data
}
```

## Thread Safety

- All operations use proper Dispatchers
- No manual synchronization needed
- StateFlow handles thread-safe state updates
- Properties file writes are synchronized

## Future Improvements

1. SQLDelight integration for books (Phase 3)
2. Encryption for sensitive settings
3. Settings sync across devices
4. Book cache optimization
5. Database migrations

## Related Documentation

- `PHASE2_STEP2_COMPLETE.md` - Detailed implementation
- `MULTIPLATFORM_MIGRATION.md` - Overall migration guide
- `shared/src/commonMain/kotlin/com/example/bottomapp/shared/backend/` - Source code


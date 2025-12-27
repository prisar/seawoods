# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Seawoods is a minimal Android document scanner app using Google ML Kit's Document Scanner API. The app demonstrates how to integrate ML Kit's full-screen document scanning with gallery import support, and includes document sharing functionality via email and other apps.

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: Single Activity with Compose Navigation
- **ML Kit**: Google ML Kit Document Scanner (play-services-mlkit-document-scanner v16.0.0)
- **Image Loading**: Coil Compose
- **Build**: Gradle with Kotlin DSL, version catalogs
- **Java Version**: Java 11
- **API Levels**: minSdk 24, targetSdk 36, compileSdk 36

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

## App Architecture

### Navigation Structure

The app uses Jetpack Navigation Compose with two screens:
1. **MainScreen** (`"main"`) - Displays scanned document image or prompts user to scan
2. **ScannerScreen** (`"scanner"`) - Launches ML Kit document scanner

### Document Scanning Flow

1. User clicks "Scan Document" button on MainScreen
2. Navigation navigates to ScannerScreen
3. ScannerScreen launches ML Kit scanner via `LaunchedEffect`
4. Scanner options are configured:
   - `SCANNER_MODE_FULL` - Full-screen scanning mode
   - `setGalleryImportAllowed(true)` - Allows importing from gallery
   - `RESULT_FORMAT_JPEG` - Results in JPEG format
5. On successful scan, first page's `imageUri` is passed back via callback
6. Navigation pops back to MainScreen with the scanned image URI
7. MainScreen displays the scanned document using Coil's `rememberAsyncImagePainter`

### Document Sharing Flow

1. After a document is scanned, a "Share via Gmail" button appears on MainScreen
2. User clicks the "Share via Gmail" button
3. `shareDocument()` function is called with the scanned document URI
4. The function converts ML Kit's `file://` URI to a secure `content://` URI using FileProvider
5. Android's share chooser opens, showing all compatible apps (Gmail, email clients, messaging apps, etc.)
6. User selects their preferred app (Gmail or any other email/messaging app)
7. The scanned document is attached as a JPEG image with pre-filled subject and message
8. **No file permissions required** - uses FileProvider for secure URI sharing with temporary read permission

### Key Implementation Details

**ML Kit Scanner Configuration** (MainActivity.kt:96-100):
```kotlin
val options = GmsDocumentScannerOptions.Builder()
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .setGalleryImportAllowed(true)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
    .build()
```

**Activity Result Handling** (MainActivity.kt:103-115):
Uses `ActivityResultContracts.StartIntentSenderForResult()` to launch the scanner and receive results. The first page's image URI is extracted from `GmsDocumentScanningResult`.

**Document Sharing with FileProvider** (MainActivity.kt:74-103):
```kotlin
fun shareDocument(context: Context, uri: Uri) {
    try {
        // Convert file URI to content URI if needed
        val contentUri = if (uri.scheme == "file") {
            val file = File(uri.path!!)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            uri
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Scanned Document")
            putExtra(Intent.EXTRA_TEXT, "Please find the scanned document attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Document")
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```
- Converts ML Kit's `file://` URI to `content://` URI using FileProvider
- Prevents `FileUriExposedException` on Android 7.0+ (API 24+)
- Uses `Intent.ACTION_SEND` to share the scanned document
- Sets MIME type to `image/jpeg` matching ML Kit's output format
- Grants temporary read permission via `FLAG_GRANT_READ_URI_PERMISSION`
- No storage permissions required - uses FileProvider for secure sharing
- Shows Android's share chooser for user to select their preferred app
- Includes error handling with try-catch block

**FileProvider Configuration** (AndroidManifest.xml:26-34):
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**File Paths Configuration** (res/xml/file_paths.xml):
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="mlkit_cache" path="." />
    <cache-path name="mlkit_docscan" path="mlkit_docscan_ui_client/" />
</paths>
```
- Configures FileProvider to access cache directory where ML Kit stores scanned documents
- `cache-path` grants access to the app's cache directory
- `mlkit_docscan_ui_client/` is the specific path where ML Kit Document Scanner saves files

**State Management**:
Image URI is stored in a `mutableStateOf<Uri?>` in MainActivity and passed to composables.

**UI Pattern**:
Uses `LocalContext.current` in MainScreen composable to obtain Context for sharing, following Compose best practices.

### FileProvider Setup for Secure File Sharing

To enable secure file sharing without storage permissions, the app uses Android's FileProvider:

**Step 1: Add FileProvider to AndroidManifest.xml**
- Add a `<provider>` element inside `<application>`
- Use authority `${applicationId}.fileprovider` for uniqueness
- Set `exported="false"` for security
- Enable `grantUriPermissions="true"` for temporary access
- Reference `@xml/file_paths` for path configuration

**Step 2: Create file_paths.xml**
- Create `res/xml/file_paths.xml` if it doesn't exist
- Define `<cache-path>` elements for ML Kit's cache directory
- Include both root cache path and ML Kit's specific subdirectory

**Step 3: Convert URIs in shareDocument()**
- Check if URI scheme is `"file"`
- If yes, use `FileProvider.getUriForFile()` to convert to content URI
- If no (already content URI), use as-is
- Add `FLAG_GRANT_READ_URI_PERMISSION` to the share Intent

**Why This is Necessary**:
- Android 7.0+ (API 24+) prohibits exposing `file://` URIs to other apps
- ML Kit Document Scanner returns `file://` URIs pointing to cache
- Direct sharing causes `FileUriExposedException` and app crash
- FileProvider creates secure `content://` URIs with temporary permissions
- Receiving apps (Gmail, etc.) can access the file without storage permissions

## Package Structure

```
com.prisar.seawoods/
├── MainActivity.kt                  # Entry point with navigation setup
│   ├── shareDocument()             # Share document via FileProvider and Intent
│   ├── MainScreen()                # Displays scan/share buttons and result image
│   └── ScannerScreen()             # ML Kit scanner launcher
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt

res/
├── xml/
│   ├── file_paths.xml              # FileProvider path configuration
│   ├── backup_rules.xml
│   └── data_extraction_rules.xml
└── ...

AndroidManifest.xml                  # Includes FileProvider configuration
```

## Dependencies

Dependencies are managed via `gradle/libs.versions.toml` version catalog:
- Compose BOM: 2025.12.01
- Navigation Compose: 2.9.6
- Coil Compose: 2.7.0
- ML Kit Document Scanner: 16.0.0

## Important Notes

- The app uses Java 11 (not Java 17 like other projects in the parent directory)
- Kotlin compiler options configured to use JVM target 11 via `compilerOptions` DSL
- No Firebase dependencies - purely ML Kit based
- Single-activity architecture with Compose navigation
- Minimal design - focuses on demonstrating ML Kit Document Scanner API and FileProvider integration

### ML Kit Document Scanner
- Results are returned as `file://` URIs pointing to temporary files in the app's cache directory
- Files are stored in `/data/user/0/com.prisar.seawoods/cache/mlkit_docscan_ui_client/`
- URIs are in JPEG format as configured in scanner options

### Document Sharing Implementation
- Document sharing requires **no storage permissions** - uses FileProvider for secure URI conversion
- FileProvider converts `file://` URIs to `content://` URIs to prevent `FileUriExposedException`
- This exception occurs on Android 7.0+ (API 24+) when sharing `file://` URIs directly
- FileProvider is configured in AndroidManifest.xml with authority `${applicationId}.fileprovider`
- File paths are configured in `res/xml/file_paths.xml` to grant access to cache directory
- Temporary read permission granted via `FLAG_GRANT_READ_URI_PERMISSION`
- Share functionality works with Gmail, email clients, messaging apps, and any app that supports receiving images
- Uses `LocalContext.current` in Compose for proper context handling instead of passing Activity references

### Common Issues and Solutions
- **FileUriExposedException**: Fixed by using FileProvider to convert file URIs to content URIs
- **Context lifecycle**: Uses `LocalContext.current` in composables instead of passing ComponentActivity
- **URI permissions**: FileProvider automatically handles URI permission grants when sharing

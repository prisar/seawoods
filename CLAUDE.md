# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Seawoods is a minimal Android document scanner app using Google ML Kit's Document Scanner API. The app demonstrates how to integrate ML Kit's full-screen document scanning with gallery import support.

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

**State Management**:
Image URI is stored in a `mutableStateOf<Uri?>` in MainActivity and passed to composables.

## Package Structure

```
com.prisar.seawoods/
├── MainActivity.kt           # Entry point with navigation setup
│   ├── MainScreen()         # Displays scan button and result image
│   └── ScannerScreen()      # ML Kit scanner launcher
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Dependencies

Dependencies are managed via `gradle/libs.versions.toml` version catalog:
- Compose BOM: 2025.12.01
- Navigation Compose: 2.9.6
- Coil Compose: 2.7.0
- ML Kit Document Scanner: 16.0.0

## Important Notes

- The app uses Java 11 (not Java 17 like other projects in the parent directory)
- No Firebase dependencies - purely ML Kit based
- Single-activity architecture with Compose navigation
- Minimal design - focuses on demonstrating ML Kit Document Scanner API
- Results are returned as URIs pointing to temporary files created by ML Kit

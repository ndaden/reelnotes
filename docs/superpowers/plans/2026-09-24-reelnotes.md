# ReelNotes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete native Android application that receives shared Instagram Reels via Android's native share sheet, extracts reel metadata and captions, structures them into actionable notes (recipes, tutorials, info, workouts) using Gemini AI and an offline heuristic NLP engine, and saves them locally in a Room database with a modern Material 3 Jetpack Compose UI.

**Architecture:** Clean Architecture with MVVM: Data Layer (Room SQLite database, OkHttp metadata fetcher, Gemini REST client), Domain Layer (Reel extraction pipeline, offline heuristic NLP analyzer, URL sanitizers), and Presentation Layer (Jetpack Compose, Material 3, Navigation Compose, ViewModels).

**Tech Stack:** Kotlin 2.1, Jetpack Compose, Material Design 3, AndroidX Room, OkHttp 4.12, Kotlinx Serialization, AndroidX Navigation Compose, Coil 2.7.

**Spec:** `docs/superpowers/specs/2026-09-24-reelnotes-design.md`

## Global Constraints
- Target Android SDK: compileSdk 35, minSdk 26, targetSdk 35.
- Package Name: `com.danstudios.reelnotes`
- Java Target: OpenJDK 21 compatibility (JVM 17/21 bytecode target).
- Standalone: Zero mandatory cloud dependencies; offline heuristic mode works completely without external API keys.
- Kotlin-first & Jetpack Compose declarative UI throughout.

---

### Task 1: Project Scaffold & Gradle Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `.gitignore`

- [ ] **Step 1: Create `.gitignore` and `gradle.properties`**
- [ ] **Step 2: Create root `settings.gradle.kts` and `build.gradle.kts`**
- [ ] **Step 3: Create `app/build.gradle.kts` with Compose, Room, OkHttp, and Kotlinx Serialization**
- [ ] **Step 4: Set up gradle wrapper and verify `./gradlew --version`**
- [ ] **Step 5: Commit**

---

### Task 2: Domain Models & URL Sanitizer

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/domain/model/NoteCategory.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/domain/model/StructuredNoteData.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/domain/model/ReelNote.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/domain/util/UrlParser.kt`
- Create: `app/src/test/java/com/danstudios/reelnotes/domain/util/UrlParserTest.kt`

- [ ] **Step 1: Write failing test `UrlParserTest`**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement `NoteCategory`, `StructuredNoteData`, `ReelNote`, and `UrlParser`**
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit**

---

### Task 3: Heuristic Offline NLP Extractor

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/domain/extractor/OfflineHeuristicExtractor.kt`
- Create: `app/src/test/java/com/danstudios/reelnotes/domain/extractor/OfflineHeuristicExtractorTest.kt`

- [ ] **Step 1: Write failing test `OfflineHeuristicExtractorTest`**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement `OfflineHeuristicExtractor` (detecting recipes, ingredients, steps, takeaways)**
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit**

---

### Task 4: Instagram Metadata Fetcher & AI Summarizer

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/data/network/InstagramMetadataFetcher.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/data/network/GeminiSummarizer.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/domain/extractor/ReelExtractionPipeline.kt`
- Create: `app/src/test/java/com/danstudios/reelnotes/data/network/GeminiResponseParserTest.kt`

- [ ] **Step 1: Write failing test for Gemini response parsing and pipeline fallback**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement `InstagramMetadataFetcher`, `GeminiSummarizer`, and `ReelExtractionPipeline`**
- [ ] **Step 4: Run test to verify passes**
- [ ] **Step 5: Commit**

---

### Task 5: Data Storage (AndroidX Room Database)

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/data/local/ReelNoteEntity.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/data/local/ReelNoteDao.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/data/repository/ReelNoteRepository.kt`
- Create: `app/src/test/java/com/danstudios/reelnotes/data/local/ReelNoteDaoTest.kt`

- [ ] **Step 1: Write unit tests for repository / DAO conversions**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement Room Entity, DAO, Converters, Database, and Repository**
- [ ] **Step 4: Run test to verify passes**
- [ ] **Step 5: Commit**

---

### Task 6: Android Share Target & Activity Integration

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/danstudios/reelnotes/ReelNotesApp.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/MainActivity.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/viewmodel/ReelNotesViewModel.kt`

- [ ] **Step 1: Configure AndroidManifest with `ACTION_SEND` intent filter and permissions**
- [ ] **Step 2: Implement Application class and ViewModel with StateFlow**
- [ ] **Step 3: Handle incoming shared URLs from Intent in `MainActivity`**
- [ ] **Step 4: Commit**

---

### Task 7: UI Layer - Theming & Navigation

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/theme/Color.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/theme/Type.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/navigation/Screen.kt`

- [ ] **Step 1: Implement Material 3 Theme with expressive color schemes**
- [ ] **Step 2: Define Navigation routes (List, Detail, Settings)**
- [ ] **Step 3: Commit**

---

### Task 8: UI Layer - Notes List, Search & Quick Add Dialog

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/screens/NotesListScreen.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/components/NoteCard.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/components/CategoryChipRow.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/components/AddReelDialog.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/components/ProcessingOverlay.kt`

- [ ] **Step 1: Implement Category Chips and Note Card with category badges**
- [ ] **Step 2: Implement NotesListScreen with search, category filtering, and empty state**
- [ ] **Step 3: Implement Add Reel Dialog with clipboard paste and processing state**
- [ ] **Step 4: Commit**

---

### Task 9: UI Layer - Note Detail & Interactive Recipe Checklist

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/screens/NoteDetailScreen.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/components/RecipeChecklist.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/components/StepList.kt`

- [ ] **Step 1: Implement interactive recipe checklist with checkboxes**
- [ ] **Step 2: Implement step progression and takeaways list**
- [ ] **Step 3: Implement NoteDetailScreen with "Open in Instagram", copy markdown, and share**
- [ ] **Step 4: Commit**

---

### Task 10: UI Layer - Settings Screen & Sample Data

**Files:**
- Create: `app/src/main/java/com/danstudios/reelnotes/ui/screens/SettingsScreen.kt`
- Create: `app/src/main/java/com/danstudios/reelnotes/data/util/SampleDataProvider.kt`

- [ ] **Step 1: Implement SettingsScreen with API Key input, language choice, and sample data loader**
- [ ] **Step 2: Implement SampleDataProvider with realistic French & English sample reels (recipe, workout, tech tip)**
- [ ] **Step 3: Commit**

---

### Task 11: End-to-End Build, Verification & Documentation

**Files:**
- Create: `README.md`
- Run: `./gradlew test`
- Run: `./gradlew assembleDebug`
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Run all unit tests with `./gradlew test`**
- [ ] **Step 2: Build debug APK with `./gradlew assembleDebug`**
- [ ] **Step 3: Verify APK generated and permissions**
- [ ] **Step 4: Write complete README.md with screenshots, architecture, and user guide**
- [ ] **Step 5: Final Git commit**

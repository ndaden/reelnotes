# ReelNotes - Architectural Design Specification

**Date:** 2026-09-24  
**Author:** Antigravity (Advanced AI Assistant)  
**Project:** ReelNotes - Android Instagram Reel to Structured Notes  
**Status:** Approved (Autonomous Execution Mode)

---

## 1. Executive Summary

**ReelNotes** is a native Android application designed to transform Instagram Reels into actionable, organized notes. When a user comes across a recipe, life hack, workout, tutorial, or news Reel on Instagram, they tap "Share" and select ReelNotes. The app receives the link, processes the content, extracts or synthesizes key information using an intelligent extraction pipeline (with Google Gemini AI support and an offline NLP heuristic parser), and saves it locally in a structured format (ingredients, steps, takeaways, tags) with a direct link back to the original reel.

---

## 2. Core User Flows

```
[ Instagram App ]
      │
      ▼ (Tap Share -> ReelNotes)
[ ShareReceiverActivity / MainActivity ]
      │
      ├─► Extract & Sanitize Reel URL
      │
      ├─► Processing Pipeline:
      │     1. Fetch Reel Metadata & Caption (Fast HTTP / Embed / Shared text)
      │     2. Analyze Content (Gemini Flash API or Smart Offline NLP Heuristic)
      │     3. Structure into Category (Recipe, Tutorial, Workout, Tips, General)
      │     4. Generate Structured JSON (Ingredients, Steps, Key Points, Tags)
      │
      ▼
[ Room SQLite Database ]
      │
      ▼
[ Jetpack Compose UI ]
      ├─► Notes List with Filters (All, Recipes, Tutorials, Workouts, Tips, Favorites)
      ├─► Live Search by text, author, tag
      ├─► Note Detail Screen (Interactive Recipe Checklist, Step-by-Step, One-Tap Original Reel)
      ├─► Export / Share to Markdown or Clipboard
      └─► Settings (Gemini API Key, Preferred Language, Fallback settings)
```

---

## 3. Technical Architecture & Tech Stack

### 3.1 Technology Stack
- **Language:** Kotlin 2.1.x
- **UI Framework:** Jetpack Compose + Material Design 3 (Material You dynamic theming)
- **Database:** AndroidX Room with SQLite (reactive `Flow<List<ReelNote>>`)
- **Networking:** OkHttp 4.12.x for lightweight, resilient HTTP metadata fetching
- **Serialization:** Kotlinx Serialization JSON
- **AI Engine:** Google Gemini Flash API (REST endpoint `gemini-2.0-flash` / `gemini-1.5-flash`) + Offline Heuristic NLP Engine
- **Image Loading:** Coil 2.7.x for Compose
- **Android Target:** minSdk 26 (Android 8.0), targetSdk 35 (Android 15), compileSdk 35
- **Build System:** Gradle 8.11 / 9.x with Android Gradle Plugin 8.10+

### 3.2 Key Architectural Components

1. **Android Manifest & Share Target:**
   - Intent Filter for `android.intent.action.SEND` with MIME type `text/plain`.
   - Single-task or standard launch mode routing to `MainActivity` with intent inspection.

2. **Reel Extraction Pipeline (`ReelExtractor`):**
   - **UrlParser:** Normalizes Instagram URLs (`/reel/ID/`, `/p/ID/`, `/reels/ID/`, removes tracking query strings `igsh`, `utm_*`).
   - **MetadataFetcher:** Fetches reel title, author, cover thumbnail, and caption text from:
     - Shared text payload (Instagram often sends caption alongside the link).
     - Instagram embed endpoint (`/p/{shortcode}/embed/captioned/`).
     - OpenGraph / JSON-LD tags.
   - **ContentSummarizer (Dual-Engine):**
     - *Online Engine:* Uses Gemini Flash to produce high-precision structured JSON according to category (e.g. ingredients with amounts, cooking instructions, prep time, difficulty).
     - *Offline Engine:* Rule-based NLP pattern matcher that detects recipe patterns (numbers, units like g, ml, tbsp, cups, cooking verbs), step sequences, and bullet points. Guarantees 100% utility even without internet or without an API key.

3. **Data Layer (`ReelNote` & `ReelNoteDao`):**
   - Stores:
     - `id`: Long (PK)
     - `reelUrl`: Clean Instagram Reel link
     - `shortcode`: Instagram media ID
     - `author`: Content creator handle (e.g. `@jamieoliver`)
     - `title`: Extracted title / descriptive title
     - `category`: `RECIPE`, `TUTORIAL`, `WORKOUT`, `TRAVEL`, `PRODUCT`, `TIPS_INFO`, `GENERAL`
     - `summary`: Short 1-2 sentence TL;DR
     - `rawCaption`: Original text extracted
     - `markdownContent`: Full formatted note in Markdown
     - `structuredJson`: Serialized `StructuredNoteData` (ingredients list, steps list, highlights)
     - `thumbnailUrl`: Cover image URL
     - `tags`: List of hashtags / topic tags
     - `isFavorite`: Boolean flag
     - `createdAt` & `updatedAt`: Timestamps

4. **UI Layer (Jetpack Compose Material 3):**
   - **ReelNotesNavGraph:** Simple, robust Compose navigation.
   - **NotesListScreen:**
     - Top search bar & category filter chips.
     - Note cards with category badges, author handle, summary, interactive favorite button, and quick reel link.
     - FAB to paste a link manually.
     - Instant processing bottom-sheet when shared from Instagram or pasted.
   - **NoteDetailScreen:**
     - Hero header with category tag and direct "Open on Instagram" button.
     - Interactive Recipe Checklist: check off ingredients while cooking or grocery shopping.
     - Step-by-Step Instruction cards.
     - Copy as Markdown / Share note.
     - Delete / Edit actions.
   - **SettingsScreen:**
     - Configure Gemini API key.
     - Choose summary language (French, English, Auto).
     - Database backup / sample data loader.

---

## 4. Error Handling & Edge Cases

1. **Private or Removed Reels:** If Instagram blocks anonymous HTML fetching, the app uses any caption included in the shared intent or prompts the user with an intuitive "Paste caption or notes" input so they never lose their saved reel.
2. **Offline Mode:** The offline heuristic analyzer extracts ingredients, steps, and summaries immediately without network access.
3. **Malformed Links:** URL sanitizer validates against Instagram patterns and alerts the user if the link is invalid.

---

## 5. Verification & Testing

- **Unit Tests:**
  - `UrlParserTest`: Tests valid/invalid URLs, shortcodes, and query parameter stripping.
  - `OfflineHeuristicExtractorTest`: Tests extraction of recipes (ingredients, quantities, steps) and info summaries from various caption styles.
  - `GeminiResponseParserTest`: Tests JSON parsing of structured AI outputs.
  - `RoomDaoTest`: Tests database insertions, queries, category filtering, favorites, and search.
- **Build Verification:**
  - `./gradlew test` executes all unit tests.
  - `./gradlew assembleDebug` produces a functional debug APK.

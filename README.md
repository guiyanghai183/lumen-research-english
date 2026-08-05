# Lumen Research English

Lumen is a native Android app for research-English learning. It combines a PDF library and reader, paper browsing, vocabulary review, an English Tutor, and local learning progress.

Current source version: **1.3.1** (`versionCode 17`).

## What is included

- PDF and ebook Library with covers, reading progress, full-screen reading, text selection, annotations, translation, and speech.
- Project Gutenberg importing and BAIR Archive reading.
- English Tutor with streamed replies, editable `memory.md`, chat history, and secure on-device API-setting storage.
- TOEFL, CET-4, and CET-6 vocabulary decks with Chinese definitions and spaced review.
- Review dashboard with Due / Learning / Memory states, unlimited manual practice, predicted recall, and dinosaur learning progress.
- In-app GitHub APK update download, integrity check, and Android system installation confirmation.

All reading, vocabulary, Tutor memory, and progress data are currently device-local. There is no account, cloud sync, or social backend in this version.

## Project layout

```text
app/src/main/java/com/lumen/researchenglish/
  data/       Room database, repositories, encrypted local settings
  domain/     review scheduler, memory model, learning-level rules
  network/    DeepSeek, Tencent, Gutenberg, and update clients
  ui/         Compose screens and AppViewModel
app/src/main/assets/
  vocabulary/ bundled TOEFL, CET-4, CET-6 data
  licenses/   bundled vocabulary-data licenses
```

## Get started after cloning

Requirements:

- JDK 17
- Android SDK 35 and Build Tools 35.0.0
- Android Studio or Gradle 8.9+

Create `local.properties` in the project root. It is intentionally ignored by Git:

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
```

Then build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

The output is at `app/build/outputs/apk/debug/app-debug.apk`.

## Secrets and local data

Do not commit API keys, signing files, `local.properties`, `.env` files, generated APKs, or build caches. The included `.gitignore` excludes them.

Enter DeepSeek and Tencent credentials only in the app's Settings screen. They are encrypted with Android Keystore and remain on the device; they are not stored in this repository.

If a key was ever shared in a chat or committed elsewhere, revoke and replace it at the provider before release.

## Bundled content notices

Source and license notices for Project Gutenberg content, WordLevel TOEFL data, and ECDICT-derived vocabulary are in [app/src/main/assets/SOURCE_NOTICES.md](app/src/main/assets/SOURCE_NOTICES.md).

## Updating the app

The app reads `update.json` from its configured GitHub repository. For each release, upload the APK and an `update.json` with the exact SHA-256 of that APK. Android will still show its own installation confirmation; silent app replacement is not possible.

## Development notes

- The UI uses Jetpack Compose; `LumenApp.kt` owns navigation and `AppViewModel.kt` owns application state.
- Room schema changes require an incremented database version and a migration.
- New local secrets belong in `SecretStore`, not source code or shared preferences in plain text.
- Run a device test after changes to reader selection, review scheduling, Tutor memory, or update installation.

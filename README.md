# Kana Dojo

Kana Dojo is a CLI-built Android Compose app for learning hiragana and katakana through short, Duolingo-style drills. It focuses on direct kana-to-sound recall, listening, writing, mistake repair, and staged progression.

The curriculum plan lives in [docs/learning-design.md](docs/learning-design.md). The UI/UX direction lives in [docs/uiux-design.md](docs/uiux-design.md).

## Current Scope

- Hiragana and katakana script switcher.
- Twenty-one lessons per script: base kana, dakuten/handakuten, small `ゃ/ゅ/ょ` blends, and special small kana or length marks.
- Lesson path with unlock gates, stage filters, daily focus, progress summary, and completion feedback.
- Practice modes for weak kana, contrast drills, sound recall, writing reps, speed rounds, both-script review, and mixed review.
- Exercise types: kana to romaji, romaji to kana, sound to kana, pair matching, and trace writing.
- Kana chart with row filters, mastery markers, contrast labels, and tap-to-hear speech.
- Local progress and mistake persistence through SharedPreferences.

## Toolchain

- JDK: 21
- Gradle wrapper: 9.6.1
- Android Gradle Plugin: 9.4.0-alpha03
- Kotlin Compose plugin: 2.4.20-Beta1
- Compile SDK: 37.1
- Target SDK: 37
- Build Tools: 37.0.0
- Compose BOM: 2026.06.01
- Material 3: 1.5.0-alpha23

The project is set up for CLI use only. It does not require Android Studio or an emulator.

## Repositories

Gradle is configured with Huawei and Aliyun Maven mirrors before official Google/Maven Central fallback repositories. This keeps dependency resolution usable on networks where the official repositories are slow or unreliable.

## Build And Test

Use the checked-in Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install On A Device

Enable USB debugging on an Android device, connect it, then run:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You can also let Gradle install the debug build:

```sh
./gradlew :app:installDebug
```

## Clean Build Outputs

```sh
./gradlew clean
```

## Source Control

This repository is initialized locally with incremental commits. No Git remote is configured yet; add one before trying to push:

```sh
git remote add origin <github-repo-url>
git push -u origin main
```

# Kana Dojo

Kana Dojo is a CLI-built Android Compose app for practicing hiragana and katakana with short lesson drills, pair matching, mistake review, speech playback, and trace practice.

The curriculum plan lives in [docs/learning-design.md](docs/learning-design.md).

## Toolchain

- JDK: 21
- Gradle: 9.6.1
- Android Gradle Plugin: 9.4.0-alpha03
- Kotlin Compose plugin: 2.4.20-Beta1
- Android SDK: Android 37.1 / Android 17
- Build Tools: 37.0.0
- Compose BOM: 2026.06.01
- Material 3: 1.5.0-alpha23

The project is set up for CLI use only. It does not require Android Studio or an emulator.

## Build

```sh
gradle :app:assembleDebug
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

## Clean Build Outputs

```sh
gradle clean
```

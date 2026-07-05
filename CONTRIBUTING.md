# Contributing

Kana Dojo is a GPL-licensed Android learning app. Contributions should keep the app focused on kana-first Japanese practice, CLI-friendly builds, and accessible Material 3 Compose UI.

## Development

Use the checked-in Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The project is designed for CLI development. Android Studio is optional.

## Pull Requests

- Keep changes focused and explain the user-facing learning or UI impact.
- Preserve existing curriculum tests or add focused tests when behavior changes.
- Avoid hardcoded user-visible strings in Compose UI; prefer Android string resources.
- Do not commit build outputs, signing keys, local SDK files, or generated APKs.

## Licensing

By contributing, you agree that your contribution is licensed under the GNU General Public License version 3.0, matching this repository.

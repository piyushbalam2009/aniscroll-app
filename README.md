# Aniscroll Flutter App

Flutter app for Android phones, Android TV, and Amazon Fire TV.

## Non-negotiable project rules

- **Anime metadata:** MyAnimeList (MAL) only.
- **Displayed titles:** English titles only. When MAL does not provide an English title, show "English title unavailable" rather than a Japanese/non-English title.
- **Streaming provider:** `zokoanime.video` only. Do not add MegaPlay or any other provider.
- **Playback:** Embedded WebView for supported ZokoAnime pages.
- **Pop-up safety:** Restrict top-level navigation and intercept unexpected windows where possible. Do not bypass DRM, access controls, or provider restrictions.
- **No secrets in source control:** Configure the MAL API client ID through a build-time value or a trusted backend. Never commit a MAL client secret.

## Current scaffold

This repository is being initialized from scratch. The first milestone is a MAL-powered ranking/search experience and a ZokoAnime-only WebView player proof of concept.

## Create the native Flutter platform folders

In GitHub Codespaces, install/configure Flutter and then run:

```bash
flutter doctor -v
flutter create . --project-name aniscroll_app
flutter pub get
flutter run
```

If Flutter reports that a platform already exists, inspect the generated files before overwriting anything.

## MAL API configuration

The app reads the MAL API client ID from the `MAL_CLIENT_ID` compile-time value. Supply it at run time/build time; do not commit credentials:

```bash
flutter run --dart-define=MAL_CLIENT_ID=YOUR_MAL_CLIENT_ID
```

For production, consider a backend proxy if you need to protect credentials or centralize rate limiting.

## Provider integration

ZokoAnime pages can change and may restrict embedding. The player is intentionally limited to `https://zokoanime.video` and its subdomains. Pop-up suppression can reduce unwanted navigation, but cannot guarantee removal of ads served inside the provider page/player.
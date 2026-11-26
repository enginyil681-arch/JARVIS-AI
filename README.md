# JARVIS AI — Android

Android-first personal assistant project. This repository currently contains the initial mobile interface prototype.

## Included in v0.1.0
- Dark navy interface with cyan and violet accents.
- Animated holographic-style JARVIS orb.
- Four feature areas: Code & Development, Library & Translation, Education, Media & Automation.
- First-launch greeting: **“Merhaba Baba.”**
- Time-based Turkish greeting on later launches.
- Android text-to-speech greeting and replies. Voice quality and gender depend on the installed Android TTS engine.
- Optional speech recognition through Android's speech recognition service after microphone permission is granted.
- Local demonstration chat only; it is not connected to an AI model yet.

## Not implemented yet
No real AI backend, web search, cloud memory/synchronization, always-on wake word, or phone setting controls are implemented in this version. These capabilities must not be represented as working until implemented and tested.

## Build requirements
- JDK 17
- Android SDK Platform 36
- Android build-tools 36.0.0
- Gradle 8.13

Build debug APK:
```bash
gradle --no-daemon assembleDebug
```
APK path: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
Pushing to `main` or manually running the **Build JARVIS AI APK** workflow builds a debug APK and uploads it as a workflow artifact for 30 days. Open the repository's **Actions** tab, select the latest successful run, and download `JARVIS-AI-Android-debug`.

This is a debug APK for testing, not a signed Play Store release. Never commit API keys or signing credentials.

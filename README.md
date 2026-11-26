# JARVIS AI — Android

Android-first personal assistant project. This repository currently contains the initial mobile interface prototype.

## Included in current Android prototype
- Dark navy interface with cyan and violet accents.
- Animated holographic-style JARVIS orb.
- Four feature areas: Code & Development, Library & Translation, Education, Media & Automation.
- First-launch greeting: **“Merhaba Baba.”**
- Time-based Turkish greeting on later launches.
- Android text-to-speech greeting and replies. Voice quality and gender depend on the installed Android TTS engine.
- Optional speech recognition through Android's speech recognition service after microphone permission is granted.
- Local demonstration chat with on-device conversation history (last 80 messages) and a clear-chat control.
- Basic local answers for time, date, help, greetings, and thanks.
- Supported voice input and Android text-to-speech output.
- Basic phone actions: open Wi-Fi, Bluetooth, network, display, sound, location, and general Settings screens; launch YouTube, WhatsApp, Spotify, Chrome, and Maps when installed.
- Launch supported Android alarm, calendar, calculator, and dialer screens.
- Open a browser search from a spoken or typed web-search command.

## Not implemented yet
No real AI backend, AI-powered web research, cloud memory/synchronization, always-on wake word, scheduled background reminders, flashlight control, or Windows app are implemented in this version. Web-search commands open a browser search; they do not mean JARVIS itself has read or summarized the results. Android intents may vary by device and installed apps.

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

# SWAAH Phase 1 — Android starter

A first prototype for Pixel 8a: push-to-talk speech recognition, Gemini text response, and Android Text-to-Speech.

## What works in this starter
- Dark black-and-gold SWAAH screen.
- Tap-to-talk using Android SpeechRecognizer.
- Hindi/English response prompting.
- Gemini API request over HTTPS.
- Spoken response through the phone's installed Android TTS voice.
- API key entry and local preference storage.

## Important limitations
- This is **not** an installed APK. Build it using the included GitHub Actions workflow or Android Studio.
- The starter uses tap-to-talk. It does not continuously listen for "Hey SWAAH".
- Android speech recognition may use the device's configured speech service and may require internet.
- TTS selects the installed `en-IN` voice where available, with `hi-IN` fallback. Android does not expose reliable voice-gender selection consistently, so a female voice is not guaranteed on every device. Install/select a female Indian English/Hindi voice in Android text-to-speech settings if available.
- The API key is stored in app preferences for this personal prototype. Do not publish this build or share the key. For production, move Gemini calls behind Firebase AI Logic / a secured backend with App Check.

## Build from your Pixel using GitHub
1. Create a GitHub repository named `swaah-android`.
2. Upload the contents of this ZIP (not the ZIP itself) to the repository.
3. Open the **Actions** tab and enable GitHub Actions if asked.
4. Run **Build SWAAH APK** from the workflow list.
5. When it finishes, open the workflow run and download the `swaah-debug-apk` artifact.
6. Install the APK on your Pixel 8a. Android may ask you to allow installs from the browser/files app.
7. Open SWAAH, grant microphone permission, paste a Gemini API key from https://aistudio.google.com/app/apikey, and tap Save API Key.

## Build locally
Use Android Studio with JDK 17 and Android SDK 36, then build the `app` debug APK.

## Next phase
Add a user-started microphone foreground service and a local wake-word engine. Android 14+ imposes microphone foreground-service restrictions, so background wake-word behavior must be implemented with explicit user start/stop and a persistent notification.

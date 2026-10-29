# Antigravity checklist — integrated Voice Doctor AI prototype

Use Google Antigravity with Android SDK/JDK17 and an Android Emulator; no physical USB phone required for these application checks.

## Clone integrated branch
```powershell
git clone --branch integration/offline-prototype https://github.com/shanmukhdatta/VoiceDoc.git
cd VoiceDoc\android
.\gradlew.bat --version
.\gradlew.bat :core:common:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -n ai.voicedoc.app/.MainActivity
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Manual acceptance cases
1. First launch: onboarding language en/te, medical boundary clear, continue, rotation, restart.
2. Bottom navigation: Home, Chat, Sources, Models, Settings; Settings -> Privacy, History, Diagnostics, About, offline voice test, microphone test.
3. Chat: ordinary questions lead to **explicit unavailable reply**, not fabricated medical advice; sample potentially urgent phrase triggers only a conservative seek-help notice, never reassurance.
4. Chat: Unicode Telugu input, max 1000 chars, clear session, input state after rotation.
5. Settings: language survives restart; opt-in encrypted history saves and reloads synthetic (non-private) test text; revoke history removes ciphertext; run `EncryptedHistoryInstrumentedTest` via connected Android tests.
6. Offline voice test: enumerates voices installed and reported network-free; if none for Telugu, UI shows unavailable. Validate actual audio in airplane mode, do not infer from enumeration.
7. Mic test: permission is requested on demand, both deny/grant flows safe, 16 kHz one-second sample diagnostic works, no audio file saved.
8. Offline privacy: no INTERNET permission; inspect merged APK manifest and verify no outbound network in airplane mode.
9. Regression: test small/large emulator screen, accessibility text sizing, app background/resume, rotation, process recreation, history disabled default.
10. Crash and performance: capture sanitized logs (do NOT share actual symptom text, contacts or serial numbers). No model RAM/speed metrics available until real adapters run.

## Limitations
An emulator cannot establish ARM64 model integration, Qualcomm NPU, Telugu ASR accuracy, hardware voice availability or clinical safety. Debug APK is installable for development **not a signed production release**.

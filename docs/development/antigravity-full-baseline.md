# Antigravity — verify the integrated Voice Doctor AI baseline
Target: Windows + JDK 17 + Android SDK 36 + Pixel API 34 x86_64 emulator.
Branch: `baseline/antigravity-qa`. The app runs fully without connecting a real phone; model/SoC benchmarks still require ARM64.

## Clone/update safely
If no local clone exists:
```powershell
git clone --branch baseline/antigravity-qa https://github.com/shanmukhdatta/VoiceDoc.git
cd VoiceDoc\android
```
If VoiceDoc already exists, first ensure all personal/local changes are saved and then:
```powershell
cd path\to\VoiceDoc
git fetch origin
git switch -c baseline/antigravity-qa --track origin/baseline/antigravity-qa
cd android
```
If the branch is already checked out, use `git pull --ff-only`. Never force reset/delete uncommitted work.

## CLI checks
```powershell
java -version
adb devices -l
.\gradlew.bat --version
.\gradlew.bat :core:common:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:installDebug
adb shell am start -n ai.voicedoc.app/.MainActivity
.\gradlew.bat :app:connectedDebugAndroidTest
```
The assembled debug APK is `android/app/build/outputs/apk/debug/app-debug.apk` after a successful build. Or fetch the `voicedoctor-debug-apk` Actions artifact for the matching commit. It is a development debug APK, not a signed release.

## Manual acceptance matrix
1. Onboarding English/Telugu, disclaimers, continue, restart, rotation.
2. Home, Chat, Sources, Models, Settings, Privacy, History, Diagnostics, About, offline TTS, mic diagnostics.
3. Chat text: typed English/Telugu/mixed script, user-entered text remains reviewable. Medical queries do NOT get fabricated AI/citations. High-risk example triggers a static contact-help notice; neither classification nor non-match is a clinical guarantee.
4. Chat on-device-only speech: on API 34, try microphone permission; if no offline recognizer/service installed, expect clear unavailability, **not remote ASR fallback**. If recognized, transcript fills input and is not sent automatically. Verify text and negations manually.
5. Chat TTS: only installed voices reporting no network; speak a static system notice with airplane mode, confirm audio. Absence of Telugu offline voice is an OPEN blocker, not a passed test.
6. History: off by default. Enable consent, send harmless synthetic text, restart, verify encrypted history; disable and erase. `EncryptedHistoryInstrumentedTest` uses its own namespace. Never share actual health text.
7. Knowledge: in Sources, import a synthetic locally built SQLite pack (using `rag/ingestion/build_pack.py` with user-created fixture). List metadata, search, delete. **No medical corpus is provided.** Verify search only displays unverified passages as reference/test data.
8. Models: without assets, clear empty states. On ARM64 only, import a properly licensed upstream .litertlm Gemma candidate and run the fixed *non-medical* greeting diagnostic. On x86 emulator, CPU benchmark may correctly remain disabled.
9. Silero VAD: optional import expected 16kHz recurrent `.onnx` model and test zero-signal output. E5: optional export `encoder.onnx` with `ml/preparation/export_e5_onnx.py`, import golden reference, compare vector parity. If no model artifacts, mark **NOT TESTED**, not PASS.
10. Diagnostics: display real device and memory/PSS measurements. Simulate low-memory and lifecycle events where feasible.
11. Offline security: inspect merged APK for absence of INTERNET permission; do not infer this solely from source manifest. Test in airplane mode after necessary local assets are installed.
12. Accessibility: try font scaling, screen rotation, Pixel API 34 and one smaller emulator screen, click/scroll all pages, note clipping/focus issues.

## Result template to paste back
```text
Branch:
Commit SHA:
Windows RAM / available disk:
Android emulator API/ABI:
Gradle unit tests: PASS/FAIL + output
Debug APK size:
assembleDebugAndroidTest: PASS/FAIL
connectedDebugAndroidTest: PASS/FAIL
Navigation + bilingual UI: PASS/FAIL
On-device recognition: PASS/FAIL/NOT AVAILABLE
Offline English TTS: PASS/FAIL/NOT AVAILABLE
Offline Telugu TTS: PASS/FAIL/NOT AVAILABLE
SQLite import/search/delete: PASS/FAIL/NOT TESTED
Consent/history encryption: PASS/FAIL
Gemma LiteRT diagnostic: PASS/FAIL/NOT TESTED
Silero ONNX diagnostic: PASS/FAIL/NOT TESTED
E5 ONNX parity: PASS/FAIL/NOT TESTED
No INTERNET permission: PASS/FAIL
Crashes/log excerpts (remove private text/IDs):
```

## Important
Local emulator success cannot establish mobile medical AI accuracy, Snapdragon NPU, real LiteRT speed, phone thermal stability, or clinical safety. This branch is **a testable engineering baseline, not the final medically approved app**.

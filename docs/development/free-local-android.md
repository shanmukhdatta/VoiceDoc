# Free local Android development — Voice Doctor AI
Checked: 2026-10-13. Android Studio, SDK, emulator, JDK and Gradle are free tools. Antigravity Individual plan is $0/month with weekly AI limits. Firebase Device Streaming Spark includes 30 minutes/month/project, NOT unlimited.

## Windows setup with Antigravity
1. Install Android Studio (https://developer.android.com/studio/) for SDK + emulator; open SDK Manager and install Android SDK Platform 36, Build Tools 35.0.0 or newer, Platform Tools, and Emulator. Accept licenses.
2. Enable CPU virtualization (Windows Hypervisor Platform / compatible acceleration). Android Studio + Emulator needs about 16GB computer RAM, with 32GB preferable. Create an x86_64 Android 15/16 Pixel virtual device in Device Manager and start it.
3. Open cloned GitHub repository in Antigravity IDE; terminal `cd android`.
4. Use JDK **17** (set JAVA_HOME to a JDK 17 installation). Android SDK normally located at `%LOCALAPPDATA%\Android\Sdk`. Android Studio can create `android/local.properties` for your local SDK path; never commit this file.
5. Antigravity terminal PowerShell:
   ```powershell
   cd android
   .\gradlew.bat --version
   .\gradlew.bat :core:common:testDebugUnitTest
   .\gradlew.bat :app:assembleDebug
   adb devices
   .\gradlew.bat :app:connectedDebugAndroidTest
   ```
6. Once build succeeds, APK appears at `android/app/build/outputs/apk/debug/app-debug.apk`. Install and launch through Android Studio Run or use `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
7. If emulator controls are unavailable in Antigravity, start the emulator from Android Studio; Antigravity's terminal can still build and call adb. Emulator requires no USB debugging on personal phone.

## IMPORTANT
- First Gradle dependency download requires internet; **application runtime** remains network-free.
- App shows a **foundation-only UI** and no medical model functions. Language toggle is real; voice/AI/RAG are deliberately unavailable, NOT fake implementations.
- No INTERNET permission or RECORD_AUDIO permission yet.
- Emulator does not establish ARM64/JNI, real hardware ML acceleration, memory, medical quality or Telugu voice availability.
- Physical-device tests and TTS license/coverage remain Phase 00 blockers; Phase 01 is not complete until Gradle tests and an emulator launch pass.

## Official references
- https://developer.android.com/studio/run/emulator
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://docs.gradle.org/current/userguide/gradle_wrapper.html
- https://www.antigravity.google/pricing
- https://firebase.google.com/docs/test-lab/usage-quotas-pricing

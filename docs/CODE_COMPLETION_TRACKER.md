# VoiceDoc — Code Completion & Verification Tracker

**Audit date:** 2026-10-29 (IST)  
**Recovery source:** `baseline/antigravity-qa` @ `8b3cc0006df508b979b0b62d9d41ffdcf7b630d0` (PR #21, open/draft).  
**Mainline at initial audit:** `main` @ `d09525626eb16d7b0b795a405cbe4039b9ef13df`.  
**Continuation branch:** `work/voicedoc-audit-hardening-20261029`.  
**Latest Android implementation commit under validation:** `3bf1e6b0800b73afa328d3fc2e7efa57b286410d` (API guard + non-deprecated locale); see CI evidence below.

## Evidence rules

- **Verified in this environment** requires a command actually executed during this audit.
- **Implemented but unverified** means source exists; it does not imply a successful new build, actual model execution or safety.
- **Partially implemented** means important interfaces/experimental components exist, but the necessary runtime connections or assets are absent.
- **Requires device/GPU/data review** means source alone cannot close that gate.
- **Blocked** must include a precise blocker.
- Previous session claims (21 Python tests, 9 Kotlin tests, ARM64 APK, whisper.cpp build, earlier conversion wrapper) **were not reproduced or independently located on this branch** and must not be reported as new passes.
- The ten-day plan's unchecked boxes remain unchecked. Project scope is `README.md`, `IMPLEMENTATION_PLAN.md` and `docs/implementation/DAY_01.md`…`DAY_10.md`, not this tracker alone.

## Evidence-based task table

| Requirement | Actual implementation / files | Verification evidence | Status | Next action |
| --- | --- | --- | --- | --- |
| Android Compose foundation, English/Telugu UI | `android/app/src/main/java/ai/voicedoc/app/ui/{VoiceDoctorApp,Pages}.kt` | Source read, no Gradle build this session | Implemented but unverified | Build, navigation/rotation/accessibility tests in Antigravity |
| Privacy manifest / network permissions | `android/app/src/main/AndroidManifest.xml` | Manifest inspected; final merged APK not inspected | Implemented but unverified | Build then `aapt dump permissions`; check no INTERNET |
| Backup & transfer exclusions | Manifest plus `res/xml/{backup_rules,data_extraction_rules}.xml` | Added this session, no Android lint/build available locally | Implemented but unverified | Run `lintDebug` and verify backup/restore behavior |
| 16 KiB native page compatibility | `scripts/audit_native_16k.py`, `android-foundation.yml` | 4 synthetic Python audit tests PASSED locally; no APK/real AAR tested | Blocked — packaged library alignment unknown | Build APK; run audit, replace/rebuild any `.so` with PT_LOAD align below 16 KiB; test on 16 KiB device |
| On-device microphone capture | `LocalPcmRecorder.kt`, `MicrophoneTestPage.kt` | Source exists; microphone not available here | Requires device/GPU/data review | Verify permission/cancel/restart/audio lifecycle |
| Android speech API compatibility guard | `android/app/src/main/java/ai/voicedoc/app/speech/OnDeviceSpeechService.kt` | Android CI at `3bf1e6b0800b73afa328d3fc2e7efa57b286410d` passed `:app:lintDebug`; API <31 returns before Android 12-only factory, no network recognizer fallback | Verified by executed tests | Physical test for Android 12+ recognizer availability and API 29–30 graceful status remains |\n| Speech recognition and editable transcription | `OnDeviceSpeechService.kt`, `Pages.kt` | Android system on-device recognizer wired; transcript remains user-editable and is not auto-submitted; no installed voice pack in CI | Requires physical device/GPU/data review | Test English/Telugu offline transcription and correction on phone |
| whisper.cpp dedicated ASR | No JNI / bundled Whisper adapter located on recovered branch | No JNI build executed here | Missing | Implement model import, native runtime, lifecycle, PCM and real-device parity before enabling |
| Language normalization & VAD | `LanguageAndVad.kt`; ONNX Silero adapter | Source exists, real model not exercised | Partially implemented | Verify normalized Telugu/English text, VAD vs labeled audio |
| Medical RAG source governance | `rag/ingestion/build_pack.py`; `LocalKnowledgePack.kt` | Source inspected; import integrity/metadata/orphan checks extended | Partially implemented | Assemble *independently* licensed and professionally reviewed source documents |
| Developer SQLite source browsing | `LocalKnowledgePack.kt`, `LocalSourcesPage.kt` | Added negative instrumented import test, **not executed** | Implemented but unverified | Run `connectedDebugAndroidTest` and import valid/invalid fixtures |
| Runtime E5 embedding | `OnnxCpuEngines.kt`, `ml/preparation/export_e5_onnx.py` | Tensor adapter present, raw-text tokenizer absent; real artifact not run | Partially implemented | Implement matching tokenizer and verify vectors/golden examples on Android |
| Retrieval-to-answer connection | `LocalKnowledgeRetriever.kt`, `OfflineTextPipeline.kt` | Common orchestration present but production default is unavailable generator | Partially implemented | Connect only approved source/embedding/SLM assets; enforce provenance/citation checks |
| Quantized SLM inference | `GemmaLiteRtDiagnostic.kt`, `ExperimentalModelStore.kt` | Diagnostic-only import/greeting; no approved weights supplied | Requires device/GPU/data review | Verify real compatible LiteRT-LM artifact on ARM64 first, then bounded answer integration |
| Production evidence-backed responses | `OfflineTextPipeline.kt`, `MedicalSafetyPolicy.kt` | Fail-closed fallback; no real-model medical answer verified | Blocked — reviewed corpus and suitable models absent | Evaluate grounding/unsupported responses before enabling |
| Urgent symptom escalation | `MedicalSafetyPolicy.kt`, fallback UI | Basic deterministic rules; no independent clinical performance testing | Partially implemented | Clinician-reviewed challenge evaluation and false-reassurance audit |
| Android offline TTS | `DeviceOfflineTts.kt`, `OfflineVoiceTestPage.kt` | Platform offline voice selection path, no playback verified here | Requires device/GPU/data review | Airplane-mode English + Telugu availability/playback; do not assume Telugu installed |
| Encrypted opt-in history | `EncryptedHistoryStore.kt`, `VoiceDoctorApp.kt` | AES-GCM Keystore source & instrumented test present, not executed here | Implemented but unverified | Test consent/restart/disable/delete including restore and failure handling |
| Model asset integrity | `ModelAssetVerifier.kt`, dev-only imports | SHA-256 and path checks in source; unsigned user-supplied imports are not authenticity | Partially implemented | Implement versioned compatible multi-asset manifest and verified publisher provenance |
| Python corpus/eval tools | `rag/ingestion/`, `ml/evaluation/`, `ml/preparation/` | Source present; existing suite not rerun here | Implemented but unverified | Run Python test suites, check fixtures and output provenance |
| Kaggle training / real evaluation | No executed training outputs or approval data supplied | No GPU session or evaluation corpus in this environment | Requires device/GPU/data review | Curate splits; run reproducible LoRA/eval and export logs/checkpoints |
| Quantization / LiteRT conversion | No verified conversion workflow for chosen current model on recovered branch | No model weights/conversion run here | Missing | Select supported model + official conversion path; publish checksums and conversion logs |
| APK build & Android instrumentation | `android-foundation.yml`; existing instrumentation source | No Android SDK/Gradle wrapper downloads here, CI not yet confirmed | Requires device/GPU/data review | Trigger Actions then run Windows/Antigravity Gradle and phone tests |
| Native library licenses and redistribution | `docs/development/`, `models/README.md` | Not independently audited for chosen future model/voices | Blocked — asset choices/rights not approved | Record each shipped dependency/model source, license, version, approval |
| Release / clinical validation | `IMPLEMENTATION_PLAN.md` gates | No signed build or clinical/device acceptance evidence | Blocked | Do not claim finished release or medical validation |

## New changes in continuation (2026-10-29)

1. Added explicit backup and Android 12+ data-extraction deny policies, retaining `allowBackup=false` and no network permission.
2. Hardened developer SQLite imports: unique staging file, SQLite quick/foreign-key checks, source provenance and cross-source chunk validation. Added an instrumented orphan-chunk rejection test (device execution pending).
3. Added a pure-Python APK native ELF/ZIP 16 KiB page alignment audit with four synthetic fixture tests. Updated Android CI to test, build, lint, upload a debug APK and then fail visibly on incompatible bundled native libraries.
4. Restored the ten-day plan and day-by-day instructions from `main` without changing their checkboxes.
5. Updated README to distinguish experimental runtime adapters from an operational medical RAG system.\n6. Added an explicit Android API 31 guard before `createOnDeviceSpeechRecognizer`; API 29–30 now report unavailable without invoking that API or selecting a network-capable recognizer. Replaced the deprecated Telugu `Locale(String, String)` constructor with `Locale.forLanguageTag`. CI verification is listed below.

### Commands genuinely run during this session

```text
cd scripts
python -m unittest discover -p 'test_*.py' -v
4 tests ran; OK (synthetic native ELF/APK fixtures)
```

**New CI evidence at implementation commit `3bf1e6b0800b73afa328d3fc2e7efa57b286410d`:** Android Foundation CI completed successfully (workflow run `40859802360`); `:core:common:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and `:app:lintDebug` passed. Compiled APK permission inspection found only RECORD_AUDIO plus AndroidX's dynamic receiver permission; INTERNET and ACCESS_NETWORK_STATE are absent. Static 16 KiB ELF/APK audit checked 14 native libraries and passed. The debug APK artifact was uploaded (run `40859802360`). This is static packaging evidence, not a 16 KiB device test.\n\n**Still not run:** connected Android instrumentation, ONNX/Gemma execution, whisper.cpp compilation, real conversion, Kaggle training, corpus approval, phone inference, clinical evaluation.

## Windows / Antigravity next validation

```powershell
git fetch origin
git switch --track origin/work/voicedoc-audit-hardening-20261029
cd android
.\gradlew.bat :core:common:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --stacktrace
.\gradlew.bat :app:connectedDebugAndroidTest
python ..\scripts\audit_native_16k.py app\build\outputs\apk\debug\app-debug.apk
adb shell getconf PAGE_SIZE
```

If the branch already exists locally use `git switch work/voicedoc-audit-hardening-20261029` and `git pull --ff-only`. Save unrelated local changes first. A debug APK is not a production or clinically approved artifact.

On the Android phone, use airplane mode after installing legitimate local assets, then check mic permission and transcript editing, missing-model fallback, source provenance, voice availability, opt-in history/restart/erasure, memory/latency, and the 16 KiB-device condition when relevant. Record failed and unavailable results explicitly.

For Kaggle, **do not assume an unverified notebook runs**. First select legally usable datasets and a model with an officially supported converter; then add/export configs, model versions, held-out evaluation, checkpoints and logs. No numerical ML or clinical metrics exist from this audit.

## Next engineering checkpoints

- [x] Resolve actual 16 KiB ELF findings from an assembled APK: static audit passed for 14 `.so` files at implementation commit `3bf1e6b0800b73afa328d3fc2e7efa57b286410d`; physical 16 KiB Android device verification remains open.
- [ ] Validate Android changes on CI/emulator and fix failures, recording run links and artifact hash.
- [ ] Build and verify a compatible offline, versioned model-pack loader and E5 tokenizer.
- [ ] Connect reviewed local evidence to bounded on-device generation and source IDs.
- [ ] Build/run genuine offline ASR and bilingual speech/answer loop on ARM64.
- [ ] Execute approved-data evaluation, convert/quantize actual models and check citations/urgency with qualified reviewers.
- [ ] Document dependency/model/voice redistribution rights and sign/test a release candidate only after all acceptance gates.

This tracker should be updated after every implementation task with paths, actual checks, commit SHA and blockers; it is not a substitute for completing the ten-day plan.

# VoiceDoc — Code Completion & Verification Tracker

**Audit date:** 2026-10-29 (IST)  
**Recovery source:** `baseline/antigravity-qa` @ `8b3cc0006df508b979b0b62d9d41ffdcf7b630d0` (PR #21, open/draft).  
**Mainline at initial audit:** `main` @ `d09525626eb16d7b0b795a405cbe4039b9ef13df`.  
**Continuation branch:** `work/voicedoc-audit-hardening-20261029`.  
**Latest verified implementation commit:** `67d587672eaf81d3386fd186ff005fbb6d718ed5`. Android and all Python CI workflows passed on this commit; this is a code/build checkpoint, not physical-device or clinical acceptance.

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
| Android Compose foundation, English/Telugu UI | `android/app/src/main/java/ai/voicedoc/app/ui/{VoiceDoctorApp,Pages}.kt` | `:app:assembleDebug` and `:app:lintDebug` passed in GitHub Actions at `67d587672eaf81d3386fd186ff005fbb6d718ed5` | Verified by executed tests | Run navigation, rotation, and accessibility checks on device |
| Privacy manifest / network permissions | `android/app/src/main/AndroidManifest.xml` | Compiled APK permission dump passed at `67d587672eaf81d3386fd186ff005fbb6d718ed5`; no INTERNET or ACCESS_NETWORK_STATE | Verified by executed tests | Verify airplane-mode behavior on device |
| Backup & transfer exclusions | Manifest plus `res/xml/{backup_rules,data_extraction_rules}.xml` | Included in passing Android build/lint; restore behavior not device-tested | Implemented but unverified | Verify backup/restore behavior on Android |
| 16 KiB native page compatibility | `scripts/audit_native_16k.py`, `android-foundation.yml` | Static APK audit passed for 14 `.so` files at `67d587672eaf81d3386fd186ff005fbb6d718ed5`; no physical 16 KiB device test | Implemented but unverified | Test on a 16 KiB-page Android device |
| On-device microphone capture | `LocalPcmRecorder.kt`, `MicrophoneTestPage.kt` | Source exists; microphone not available here | Requires device/GPU/data review | Verify permission/cancel/restart/audio lifecycle |
| Android speech API compatibility guard | `android/app/src/main/java/ai/voicedoc/app/speech/OnDeviceSpeechService.kt` | Android CI passed `:app:lintDebug`; API <31 returns before Android 12-only factory, no network recognizer fallback | Verified by executed tests | Physical test for Android 12+ recognizer availability and API 29–30 graceful status remains |
| Speech recognition and editable transcription | `OnDeviceSpeechService.kt`, `Pages.kt` | OS on-device recognizer wired; transcript remains editable and is not auto-submitted; actual offline language packs not tested | Requires physical device/GPU/data review | Test English/Telugu offline transcription and correction on phone |
| whisper.cpp dedicated ASR | No JNI / bundled Whisper adapter located on recovered branch | No JNI build executed here | Missing | Implement model import, native runtime, lifecycle, PCM and real-device parity before enabling |
| Language normalization & VAD | `LanguageAndVad.kt`; ONNX Silero adapter | Source exists, real model not exercised | Partially implemented | Verify normalized Telugu/English text, VAD vs labeled audio |
| Medical RAG source governance | `rag/ingestion/build_pack.py`; `LocalKnowledgePack.kt` | Source inspected; import integrity/metadata/orphan checks extended | Partially implemented | Assemble *independently* licensed and professionally reviewed source documents |
| Developer SQLite source browsing / corpus builder | `LocalKnowledgePack.kt`, `LocalSourcesPage.kt`, `rag/ingestion/build_pack.py` | Five Windows Python ingestion tests pass, including empty input, staging cleanup, and preserving an existing output; Android importer instrumentation is still not executed | Partially implemented | Run connected tests and import a valid developer pack on Android |
| Runtime E5 embedding | `OnnxCpuEngines.kt`, `ml/preparation/export_e5_onnx.py` | Tensor adapter present, raw-text tokenizer absent; real artifact not run | Partially implemented | Implement matching tokenizer and verify vectors/golden examples on Android |
| Retrieval-to-answer connection | `LocalKnowledgeRetriever.kt`, `OfflineTextPipeline.kt` | Common orchestration present but production default is unavailable generator | Partially implemented | Connect only approved source/embedding/SLM assets; enforce provenance/citation checks |
| Quantized SLM inference | `GemmaLiteRtDiagnostic.kt`, `ExperimentalModelStore.kt`, `LiteRtGroundedGenerator.kt` | Bounded LiteRT-LM adapter compiled in Android CI at `67d587672eaf81d3386fd186ff005fbb6d718ed5`; common tests validate token budget and citation parsing. No approved weights supplied and adapter is not wired to production UI | Partially implemented | Verify a compatible reviewed pack on ARM64; connect only after safety evaluation |
| Production evidence-backed responses | `OfflineTextPipeline.kt`, `MedicalSafetyPolicy.kt` | Fail-closed fallback; no real-model medical answer verified | Blocked — reviewed corpus and suitable models absent | Evaluate grounding/unsupported responses before enabling |
| Urgent symptom escalation | `MedicalSafetyPolicy.kt`, fallback UI | Basic deterministic rules; no independent clinical performance testing | Partially implemented | Clinician-reviewed challenge evaluation and false-reassurance audit |
| Android offline TTS | `DeviceOfflineTts.kt`, `OfflineVoiceTestPage.kt` | Platform offline voice selection path, no playback verified here | Requires device/GPU/data review | Airplane-mode English + Telugu availability/playback; do not assume Telugu installed |
| Encrypted opt-in history | `EncryptedHistoryStore.kt`, `VoiceDoctorApp.kt` | AES-GCM Keystore source & instrumented test present, not executed here | Implemented but unverified | Test consent/restart/disable/delete including restore and failure handling |
| Model asset integrity and pack preparation | `ModelAssetVerifier.kt`, `ml/preparation/model_manifest.py` | Schema-v1 multi-asset/runtime/language/embedding-dimension validator; five preparation tests pass locally and CI. Android production pack installer and publisher authentication remain absent | Partially implemented | Build importer around approved publisher metadata and test real assets |
| Python corpus/eval tools | `rag/ingestion/`, `ml/evaluation/`, `ml/preparation/` | All Python workflows passed on `67d587672eaf81d3386fd186ff005fbb6d718ed5`; local suites: 5 ingestion + 5 preparation + 7 evaluation + 4 native audit tests | Verified by executed tests | Replace only synthetic fixtures with approved evaluation data after review |
| Kaggle training / real evaluation | No executed training outputs or approval data supplied | No GPU session or evaluation corpus in this environment | Requires device/GPU/data review | Curate splits; run reproducible LoRA/eval and export logs/checkpoints |
| Quantization / LiteRT conversion | No verified conversion workflow for chosen current model on recovered branch | No model weights/conversion run here | Missing | Select supported model + official conversion path; publish checksums and conversion logs |
| APK build & Android instrumentation | `android-foundation.yml`; existing instrumentation source | APK and Android-test APK builds passed on `67d587672eaf81d3386fd186ff005fbb6d718ed5`; connected tests were not run | Requires physical device/GPU/data review | Run `connectedDebugAndroidTest` on emulator/device |
| Native library licenses and redistribution | `docs/development/`, `models/README.md` | Not independently audited for chosen future model/voices | Blocked — asset choices/rights not approved | Record each shipped dependency/model source, license, version, approval |
| Release / clinical validation | `IMPLEMENTATION_PLAN.md` gates | No signed build or clinical/device acceptance evidence | Blocked | Do not claim finished release or medical validation |

## New changes in continuation (2026-10-29)

1. Added explicit backup and Android 12+ data-extraction deny policies, retaining `allowBackup=false` and no network permission.
2. Hardened developer SQLite imports: unique staging file, SQLite quick/foreign-key checks, source provenance and cross-source chunk validation. Added an instrumented orphan-chunk rejection test (device execution pending).
3. Added a pure-Python APK native ELF/ZIP 16 KiB page alignment audit with four synthetic fixture tests. Updated Android CI to test, build, lint, upload a debug APK and then fail visibly on incompatible bundled native libraries.
4. Restored the ten-day plan and day-by-day instructions from `main` without changing their checkboxes.
5. Updated README to distinguish experimental runtime adapters from an operational medical RAG system.
6. Added an explicit Android API 31 guard before `createOnDeviceSpeechRecognizer`; API 29–30 now report unavailable without invoking that API or selecting a network-capable recognizer. Replaced the deprecated Telugu `Locale(String, String)` constructor with `Locale.forLanguageTag`.
7. Fixed the Windows corpus-builder test failure by closing SQLite connections, rejecting empty corpora, and building in unique staging databases before atomic publication. Added failure-cleanup and no-overwrite tests.
8. Added a schema-v1 multi-file model-pack validator with declared runtime/version, languages, embedding dimension, required roles/formats, path/size/hash validation, and duplicate/empty-pack rejection. This is a preparation preflight, not an Android pack installer or publisher-authentication mechanism.
9. Added a dormant bounded LiteRT-LM adapter to the shared structured generator interface. It uses the pinned API's max-output-token setting, disables tool calling and thinking, serializes access/lifecycle, and requires explicit source markers. The app default remains fail-closed; the adapter is not connected to production UI or approved assets.

### Commands genuinely run during this session

```text
cd scripts
python -m unittest discover -p 'test_*.py' -v
4 tests ran; OK (synthetic native ELF/APK fixtures)
```

**CI evidence at verified implementation commit `67d587672eaf81d3386fd186ff005fbb6d718ed5`:** Android Foundation CI run `40863419589` passed `:core:common:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`, merged APK permission inspection, and static 16 KiB audit (14 libraries). The corpus (`40863419418`), preparation (`40863419420`), and metrics (`40863419411`) workflows also passed. The debug APK artifact is attached to the Android run. The CI emitted toolchain deprecation notices for Gradle 8.13, `setup-java@v4`, and Node 20; no Android lint errors remained.

**Local tests in this continuation:** corpus ingestion 5 passed (including Windows temp cleanup); model preparation 5 passed (including versioned packs); evaluation 7 passed; native audit 4 passed. `git diff --check` passed. Local Gradle could not resolve uncached AARs/JARs because this environment reported network unreachable; GitHub Actions compiled and linted the implementation successfully.

**Still not run:** connected Android instrumentation, real ONNX/Gemma execution, whisper.cpp compilation, real model conversion, Kaggle training, corpus approval, phone inference, clinical evaluation.

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

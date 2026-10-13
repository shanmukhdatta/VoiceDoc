# Phase 00 — Technology stack and integration contracts
Date: 2026-10-13 · English/Telugu only · **Proposed, not built.**

## Application and build
- Native **Kotlin + Jetpack Compose**; MVVM state, Clean Architecture at external boundary, Coroutines/Flow.
- Gradle Kotlin DSL + wrapper + Android Gradle Plugin pinned together in Phase 01; Android Studio stable; JDK 17 baseline and version compatibility check.
- Hilt for DI after Gradle version pinning, Room/SQLite for metadata and searchable text, DataStore for non-sensitive preferences, Android Keystore-backed authenticated encryption for optional history.
- Native `arm64-v8a` C/C++ with NDK/CMake and JNI only when needed; no 32-bit support promised.
- JUnit + JVM unit tests, Android instrumentation, Compose tests, benchmark tests, static analysis and dependency/license scanning.

## Modules and contracts
```text
:app / :feature:chat
:core:audio             AudioRecord + VAD + bounded buffers
:core:asr               SpeechRecognizerEngine (Whisper JNI; candidate Indic path)
:core:language          Script/language/normalization preserving negation, quantities
:core:safety            SafetyEngine (independent input/output, never model-only)
:core:embedding         EmbeddingEngine (E5-small ONNX)
:core:rag               HybridRetriever (FTS + exact cosine + provenance)
:core:llm               LanguageModelEngine (LiteRT-LM, replaceable)
:core:tts               TextToSpeechEngine (Android platform / sherpa-onnx)
:core:data              Room/SQLite KB + optional history storage
:core:model-manager     Version + SHA256 + license/asset compatibility manifest
:core:metrics           Per-module latency, PSS/RSS, thermal, battery
```
Kotlin app orchestrator depends on interfaces, not libraries. All engines need `initialize/load`, `capabilities`, cancellation, `release` and structured errors. Audio in 16kHz mono PCM; text in Unicode UTF-8 normalized carefully; tokens/chunks identified with stable IDs. UI never executes heavy inference on main thread.

## Correct high-level route
```text
Mic -> VAD -> ASR -> user-confirmable transcript + conservative normalization
                         |
                  independent safety/intent routing
                    | emergency/unsafe -> reviewed static escalation or uncertainty fallback
                    | information question
                    v
         Embed query -> FTS + cosine retrieval -> licensed source chunk identifiers
                    -> bounded prompt -> 1B local LLM -> structured output validator
                    -> provenance/claim checks -> screen text -> optional offline TTS
```
Text input must take the same safety/retrieval path as ASR input. Urgent routing must not require LLM or successful retrieval. Generation text must be **buffered and validated before TTS**; speculative unsafe partial tokens must not be spoken. Streaming may be displayed only with an explicit provisional status until validated.

## RAG indexing, ranking and storage
Offline desktop ingestion: fetch explicitly approved source/version -> verify rights -> extract/clean/dedup -> semantic chunk -> metadata -> E5-small embedding -> normalize -> serialize vectors plus source manifest. At runtime: Android SQLite FTS5 lexical candidates, CPU E5-small ONNX query vector, exact dot product across precomputed L2-normalized vectors, reciprocal rank fusion with calibrated thresholds and diversity cap, clinician-approved passage selection, evidence IDs. Retrieval threshold must be calibrated on labeled bilingual query sets, never fabricated.
Vector storage choices: versioned read-only binary matrix plus SQLite offsets is more predictable than unverified SQLite vector extension; Room owns metadata, not a magic ANN dependency. Index checksums and source/license audits are mandatory.

## Medical safety design
- Independent rule-based risk and intent pre-check; bounded question classification. Escalation templates **do not give definitive diagnoses or promise to exclude emergencies**.
- Input checks for uncertain ASR, ambiguous units, missing negation, minor-vulnerable contexts, urgent concern patterns in both languages.
- Output JSON schema (validated locally): `language`, `intent`, `answer`, `source_ids`, `uncertainty`, `escalation`. Failure -> static safe text, not unchecked raw model answer.
- Source IDs must exist in retrieved licensed chunk set and substantively support the answer. Identifiers alone do not prove that a claim is supported; evaluate entailment/factuality conservatively, with clinician-reviewed evaluation and abstention.
- Versioned policies, scripted counterexamples, safety-negative tests, and human review required. No autonomous diagnosis, medication prescription, or emergency exclusion.

## Privacy and security
No Internet permission required for offline-only runtime flavor; audit all transitive SDKs, crashes, analytics, permissions and network attempts. Models and KB installed via pre-packaged/manual transfer pathway; if an online asset downloader is added later it must be explicit and excluded from offline inference. Never log raw audio, symptoms or health text to Logcat by default. Encrypt opt-in retained history using an authenticated cipher with Android Keystore-protected key; avoid relying on deprecated security wrappers without verifying their support. Secure delete-history UX; backups disabled/exclude sensitive files; screenshot and clipboard risks considered. Prevent prompt injection from retrieved documents by data/policy separation.

## Model preparation (non-Android)
Python isolated environment + PyTorch, Hugging Face Transformers, PEFT/Accelerate for later experiments; ONNX export and model tokenization golden references; LiteRT-LM packaging follows official supported conversion instructions. **A fine-tuned checkpoint is not automatically LiteRT-LM compatible.** Quantization/conversion feasibility must be proven on exact revision. Keep transformation scripts + pinned version+SHA+licenses instead of committing binaries.

## Dependency compatibility policy
Pin Gradle/JDK/AGP/Kotlin/Compose/NDK, ONNX Runtime AAR, LiteRT-LM AAR, whisper.cpp commit, sherpa-onnx commit, and model versions in Phase 01 after minimal compatibility projects pass. Each version must be accompanied by license audit, arm64 ABI inspection, dependency tree and device smoke-test. Don't assume simultaneous native libraries are free of linker/allocator conflicts.

## Source links
- Android Gradle compatibility: https://developer.android.com/build/releases/gradle-plugin
- LiteRT-LM Kotlin API: https://developers.google.com/edge/litert-lm/android
- whisper.cpp Android: https://github.com/ggml-org/whisper.cpp
- ONNX Runtime Android: https://onnxruntime.ai/docs/build/android.html
- sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
- Room: https://developer.android.com/training/data-storage/room
- Android Voice API: https://developer.android.com/reference/android/speech/tts/Voice

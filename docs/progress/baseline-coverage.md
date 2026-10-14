# Voice Doctor AI — Complete baseline code audit
Date: 2026-10-14. Branch: `baseline/antigravity-qa`.
**Status: Technical prototype code present; CI verification and Android Emulator acceptance required. NOT a completed or clinically validated medical AI product.**

This branch includes all accumulated code from Phase 02 onward in PRs #4–#20, on top of the accepted Phase 00/01 code on `main`. English/Telugu ONLY; no Tamil in this release.

| Phase | Now present in source | Missing / must not claim |
|---|---|---|
| 00 feasibility | Official tech/licensing review | ARM64 model benchmarks, commercially viable Telugu TTS, expert corpus approval |
| 01 foundation | Verified Kotlin Compose/Gradle foundation | Recheck with new dependencies |
| 02 UI | Welcome, home, text/voice input, Sources, Models, Settings, Privacy, History, Diagnostics, TTS and mic test | Emulator accessibility/layout review |
| 03 LLM | **Real LiteRT-LM 0.18.0 Kotlin CPU Engine adapter**, local .litertlm experimental import; benign fixed greeting inference diagnostic | No signed approved Gemma3-1B asset; no clinically grounded production LLM; real ARM64 performance unknown |
| 04 source governance | Python approved-rights pack builder, Android SQLite private corpus importer/provenance viewer | Actual rights-verified, physician-reviewed bilingual source set not provided |
| 05 RAG | lexical SQLite query + pure Kotlin hybrid cosine/RRF, ONNX E5 1.30.0 token-tensor execution + masked mean pooling, golden-reference evaluation UI | Android raw-text SentencePiece tokenizer parity, semantic retrieval accuracy, trusted KB-to-medical-answer activation |
| 06 safety | independent bilingual example risk routing, static emergency notice, source-ID/output format checks | Clinical safety/false reassurance validation; keyword miss cases; claim-level entailment |
| 07 orchestrator | fail-closed input->retrieval->model->citation structure & JVM tests | Actual production model and approved source integration |
| 08 ASR/VAD | Microphone permission, PCM 16kHz capture, energy detector, **real Android on-device-only SpeechRecognizer**, Silero CPU ONNX adapter with state | Whisper.cpp JNI model/transcription; Telugu medical terms and code-switch accuracy; licensed Silero asset |
| 09 TTS | Android platform offline voice enumeration + playback, optional voice output from static system responses | No guaranteed offline Telugu installed voice; no commercially-approved redistributable backup |
| 10 privacy/assets | Opt-in Keystore AES-GCM stored conversation, consent/revoke/delete, no INTERNET permission; model copy/hash checks | Signing of trusted manifests, independent security assessment and key migration; app experimental model file not authenticated |
| 11 voice interface | Device-only ASR fills reviewable transcript -> static safety fallback text -> offline TTS when voice installed | True medical speech-to-speech RAG/LLM generation not approved |
| 12 evaluation | Python WER/CER, Recall@K, urgency recall scripts and CI synthetic tests; Android memory snapshot | Real en/te dataset, clinician labels, safety coverage, real device benchmark |
| 13 ML/optimization | Verified rights schema, deterministic group-held-out dataset split, E5 ONNX export tool, manifest checker | Actual fine-tuning, QLoRA/LoRA weights, Whisper quantization, Gemma LiteRT conversion, QNN NPU validation, quality regressions |
| 14 release | Debug APK compilation in GitHub Actions with downloadable artifact, full emulator test sources | Signed production APK/AAB, Play policy/privacy reviews, supported-device matrix, compliance/clinical approvals |

## Terminology
**Implemented code:** source exists. **CI verified:** compilation/unit tests executed on GitHub runner. **Emulator verified:** user runs Antigravity tests and shares results. **Model verified:** exact licensed weights load and measured in correct runtime; **none of the unprovided assets are model verified**. **Clinically validated:** independently qualified safety evaluation; **not achieved**.

## Key security and product restrictions
- Medical chat never consumes arbitrary experimental Gemma output or unverified imported clinical text. It returns a clear unavailable/seek-help message instead of fabricating a diagnosis.
- User-provided model and SQLite file imports are developer diagnostics. Self-claimed licenses/reviewers and SHA of imported bytes are NOT sufficient to declare source authenticity, redistribution rights or physician validation.
- REAL LiteRT/ONNX and OS speech APIs are called only on explicit diagnostic interactions; import/output size and capabilities checked; ARM64 device required for experimental LiteRT benchmark.
- No INTERNET permission. Initial dependency downloads/SDK/model acquisitions on the development computer require network, not runtime inference.
- App not ready to treat health emergencies or exclude them. Seek qualified professional/emergency assistance for real urgent concerns.
- Telugu and English voices must be audibly tested in airplane mode on each supported device.

## External acceptance still pending
1. GitHub final branch Android CI including test APK packaging.
2. User Antigravity x86_64 emulator: first launch, all navigation, text/voice input availability, local search, privacy/Keystore tests, TTS flags, model diagnostic graceful failures.
3. Real ARM64 phone: actual Gemma, ONNX and speech assets with proper licenses, RAM/latency and Telugu quality.
4. Reviewed bilingual medical sources + professional safety evaluations before any real health advice generation is enabled.

See [the Antigravity test checklist](../development/antigravity-full-baseline.md).

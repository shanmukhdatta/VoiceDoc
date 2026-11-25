# Voice Doctor AI — End-to-end phase coverage & release blockers
Updated 2026-10-14. **This is an implementation audit, not a declaration of product completion.**

Initial release: **English/Telugu only**, offline inference, Android. General health information, never autonomous diagnosis, treatment prescription or emergency exclusion.

| Planned phase | Implemented / PR | Verified evidence | Remaining work |
|---|---|---|---|
| 00 Feasibility | PR #1 merged | Technical docs/source inspection | Physical Gemma/ASR test, Telugu TTS redistribution, licenses |
| 01 Android toolchain | PR #2 + #3 merged | CI and user Antigravity emulator acceptance | Repeat for final integrated branch |
| 02 Android UI/navigation | PR #4 | CI compilation + JVM tests | Emulator full navigation/accessibility QA |
| 03 On-device SLM | PR #5: interface/assets integrity **only** | CI/JVM tests | LiteRT-LM Android runtime adapter, quantized Gemma licensed asset, phone benchmark |
| 04 Medical knowledge | PR #6: governed SQLite pack builder | Python synthetic CI | Clinician-reviewed English/Telugu corpus, source reuse/translation rights |
| 05 Embeddings / hybrid RAG | PR #6: lexical + exact cosine/RRF primitives | CI/JVM synthetic tests | E5-small ONNX Android tokenizer/pooling, SQLite runtime integration, QA |
| 06 Independent safety | PR #7: rules and mechanical output gate | CI/JVM synthetic tests | Clinical validator review, urgent recall/false reassurance, claim support checking |
| 07 Text pipeline | PR #12: orchestrator, fail-closed generator | CI tests pending | Working production SLM/RAG adapter and clinical grounding |
| 08 Offline ASR | PR #10: mic capture only | Android CI pending | Whisper.cpp JNI, VAD model, en/te transcription/medical WER, code-switch |
| 09 Offline TTS | PR #8: Android offline voice probe | Android CI | Offline English/Telugu voice playback, licensed fallback if te missing |
| 10 Memory/privacy/assets | PR #9 + PR #5: Keystore history + checksums | Android compile/JVM; device instrumentation pending | Secure review, model pack import/manifests/signing, key rotation |
| 11 Voice-to-voice | NOT IMPLEMENTED | None | Audio/VAD/ASR -> RAG -> SLM -> safety -> TTS integrated and tested |
| 12 Multilingual/clinical evaluation | PR #11: pure Python metric harness | Python synthetic CI | Held-out clinician case studies, adverse-case audits, real performance evaluation |
| 13 Optimization/training | NOT IMPLEMENTED | None | Kaggle LoRA/QLoRA if justified, quantization, conversion, CPU/GPU/NPU benchmark |
| 14 Final release | NOT IMPLEMENTED | None | Signing, installable release APK/AAB, security and accessibility audit, accepted safety/privacy/license gates |

## Non-negotiable release gates
- Production model asset hashes, licenses, tokenizer and exact runtime versions verified. No artificial successful model inference.
- Independent bilingual clinician-reviewed medical corpus, translations and safety challenge sets. Model speed is not a medical-quality claim.
- English/Telugu ASR and offline TTS verified in airplane mode on supported Android hardware.
- Memory/latency/thermal measured on real ARM64 devices (OnePlus 11R candidate, exact RAM to confirm); x86_64 emulator performance is not representative.
- Data-rights and voice-model distribution approvals for commercial use. MMS Telugu non-commercial baseline excluded.
- Encrypted-history storage tested for consent/restart/deletion. Deletion does not imply forensic erasure of flash.
- Signed release artifact, install/upgrade/rollback test and appropriate professional review before real-world health-information reliance.

## What the prototype can accurately demonstrate
- Android native bilingual navigable UI, text session, onboarding, privacy, no Internet permission.
- Local static abstention/urgency examples, not reliable emergency assessment.
- Optional Android Keystore-encrypted history (instrumentation acceptance needed).
- Local microphone diagnostic (not speech-to-text).
- Platform TTS offline-voice availability probe (not guaranteed Telugu voice).
- Standalone licensed-source pack tooling, retrieval primitives, model integrity utilities and testable fail-closed text orchestration.

**Do not merge all PRs or label any blocked phase complete merely because compilation passes.**

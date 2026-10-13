# Phase 00 — Feasibility and technical preparation
Date: 2026-10-13
Owner: Voice Doctor AI engineering
Branch: `phase-00/feasibility-baseline`
**Status: AWAITING VERIFICATION — NOT COMPLETE**

## Scope frozen
- English/Telugu only (no Tamil in this release).
- Offline-after-install Android health information, **not autonomous diagnosis/prescribing/emergency exclusion**.
- Phase 00 consists of research, architecture and compatibility checks; no app construction yet.
- `main` must remain unchanged until reviewed/merged.

## Completed work (documentary, NOT a claim of executable functionality)
- [x] Read actual repository README (architecture proposal) and recursive default-branch tree (README.md only).
- [x] Checked upstream Android deployment documentation for Gemma 3 1B / LiteRT-LM; verified model-specific **32K** base context and reference 529 MB QAT variant.
- [x] Checked whisper.cpp Android path and multilingual tiny/base disk sizes; flagged Telugu medical ASR unknown.
- [x] Checked Silero VAD ONNX license and ONNX Runtime Android path.
- [x] Checked E5-small model card/384-dim/ONNX listing/MIT and noted 471 MB FP32 weight footprint.
- [x] Checked Android TextToSpeech offline-voice API; identified unresolved Telugu TTS release blocker and excluded CC-BY-NC MMS model from commercial baseline.
- [x] Checked AI4Bharat IndicConformer and IndicF5 as research candidates, **not validated Android substitutes**.
- [x] Prepared reference architecture, preliminary storage/RAM budgeting, safety/privacy/rights rules and bounded validation plan.
- [x] Created dedicated phase branch; no unrelated files modified.
- [x] Added five phase documents and optional basic device-info helper script (once verified in branch).

## Not completed
- [ ] Receive sanitized target phone SoC / RAM / Android API / available storage.
- [ ] Pin a real working Kotlin/AGP/Gradle/LiteRT-LM/ONNX/NDK dependency combination via isolated test project (Phase 01 or compatibility spike).
- [ ] Test exact Gemma .litertlm artifact on device, including CPU fallback.
- [ ] Test whisper multilingual base/tiny and Telugu health vocabulary accuracy on device.
- [ ] Test E5-small ONNX tokenizer/reference parity and Telugu medical retrieval.
- [ ] Confirm actual offline **English and Telugu** TTS voice. If Telugu unavailable, select compatible redistributable model and prove inference.
- [ ] Measure process peak PSS, load/TTFT, decode speed, ASR RTF, battery/thermal, storage and restart handling.
- [ ] Establish permitted medical source corpus, translation quality review and credible clinician-reviewed safety benchmark.
- [ ] Run airplane-mode all-components smoke test after APK exists (later phase; Phase 00 can establish capability per-component).

## Current blockers and response
1. **TTS Telugu (release critical):** platform voice on target phone unverified; sherpa-onnx is a framework, not proof that a Telugu voice model is available, fast and licensed. No fallback substitution permitted without explicit architectural review.
2. **Language/safety quality (release critical):** a 1B general SLM and tiny/base ASR need measured code-switch, accent and terminology reliability; safety rules and human review required.
3. **Hardware unspecified:** cannot declare supported minimum or real-time performance.
4. **Asset rights:** Gemma model terms, each TTS weight/voice license and each medical source/translation license must be independently approved.
5. **Offline model supply:** decide whether assets are preinstalled, bundled or imported offline; large APK/store limits and storage staging must be tested.

## Evidence & limitations
All upstream benchmark results come from third-party reference documentation, **not VoiceDoc test executions**. No Android build, Kaggle training, model conversion, on-device benchmark, audio evaluation or clinical testing has been completed in this phase. The optional `collect_device_info.py` reports only device properties via adb; it is **not** a compatibility or inference test.

## Phase 01 entry criteria (review gate)
- [x] README inspected and target en/te-only scope fixed.
- [x] Clearly separated verified upstream deployment paths, provisional design decisions and unknowns.
- [x] Modular fallback boundaries, safe product claims, knowledge licensing and privacy rules documented.
- [x] Estimated initial model storage and memory with source/assumptions.
- [ ] Hardware baseline for **at least one actual Android device** recorded.
- [ ] Exact Phase 01 toolchain versions selected following official compatibility matrix.
- [ ] CPU reference Gemma LiteRT-LM artifact load demonstrated on intended device or documented small independent validation device.
- [ ] Telugu TTS solution/owner/experiment approved; still a release blocker until verified.
- [ ] Phase 00 risks accepted by project review; don't present pending as done.

**Recommendation:** Phase 01 can be drafted while Phase 00 tests continue, but it must start with an isolated build/runtime compatibility spike and cannot be marked integrated or complete until verified. Do not label Phase 00 COMPLETE while required hardware and TTS feasibility gates are pending.

## External handoff (user involvement only where tools lack hardware)
Run: `python scripts/phase00/collect_device_info.py --output device-report.json` after Android platform-tools are installed and USB debugging is authorized. Inspect the report and share non-identifying hardware fields only (do not post serials, ADB identifiers, raw health text). Android voice enumeration and runtime benchmark require a small isolated Android compatibility app during later testing.

## Next review
Record device facts, test artifact hashes, model+runtime exact versions, error logs, and test outcomes in a dated append-only result record before reconsidering the phase gate.

## Verification run (2026-10-13, local sandbox; not Android hardware)
- Python helper exact Git blob SHA: `44d60c093644c1c6f7ae5457fec7174cede35c8c` (matched GitHub).
- `python -m py_compile scripts/phase00/collect_device_info.py` **PASS** (local source hash matched GitHub).
- `python scripts/phase00/collect_device_info.py --help` **PASS**.
- `python -m unittest discover -s tests/phase00 -p 'test_*.py' -v` **3/3 PASS**, with only mocked ADB responses; initial test harness argument-handling bug was corrected before commit.
- The sandbox could not clone GitHub over network; final files and SHA were checked through the connected GitHub API. No physical phone, model, Android build, or network isolation test has run.

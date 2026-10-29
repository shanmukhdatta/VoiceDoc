# Phase 03 — Language model/runtime preparation
**STATUS: PARTIAL FOUNDATION ONLY — SLM INFERENCE NOT IMPLEMENTED.**
Branch: `phase-03/offline-engine-contracts` (stacked on Phase 02)

## Implemented
- Explicit VAD/ASR/embedding/retrieval/SLM/TTS capability interfaces with coroutine-compatible suspend calls, typed data and explicit unavailable exception.
- SHA-256 model asset integrity check requiring trusted metadata, confirmed redistribution permission, expected file size, format and safe containment.
- JVM tests cover correct bytes, hash mismatch, size mismatch, missing assets, traversal, and unapproved rights.

## Not claimed
- No LiteRT-LM AAR bound yet, no Gemma 3 1B model asset downloaded/redistributed, no CPU/GPU/QNN benchmark, no physical-phone inference, no quantization path proven.
- No Whisper, Silero, ONNX Runtime, or sherpa-onnx model runtime integrated yet.
- These are **tested contracts and integrity utilities**, not a functional local LLM.

## Gate
- CI tests pass.
- Later implement LiteRT-LM adapter against official Kotlin API version, assets and license approvals.
- Real-device memory/latency + en/te quality evaluation needed before marking this phase complete.

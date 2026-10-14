# Phase 09 — Offline voice capability
**Status: PARTIAL, real Android platform engine only.**

- Added Android TextToSpeech engine wrapper that enumerates only installed en/te voices with `isNetworkConnectionRequired=false` and attempts local playback.
- Added settings navigation to test voices. Honest unavailable state where no voice exists.
- Requires emulator/real-device **airplane-mode listening test** to verify audio; voice list alone is insufficient.
- No redistributable Telugu ONNX voice chosen; commercial licensing and fallback still blocked.
- No speech-to-speech integration or Whisper ASR. This PR is not final Phase 09 acceptance.

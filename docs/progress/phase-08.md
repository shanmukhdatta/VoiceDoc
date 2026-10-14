# Phase 08 — Local speech input preparation
**Status: PARTIAL — microphone diagnostic implemented; offline ASR not integrated.**

- Explicit Android RECORD_AUDIO runtime permission prompted only from microphone diagnostic screen.
- Native AudioRecord captures 1 second PCM16 16 kHz mono in-memory on IO dispatcher; releases and wipes buffer; returns non-identifying sample count/RMS.
- No raw audio written to storage or sent over network.
- No Whisper JNI, IndicConformer, VAD inference, transcription, medical term WER/CER, or multilingual code-switch benchmarks yet.
- Validation: emulator/phone grant/deny, 16 kHz capture, no app crash, mic indicator and privacy permission audit. Real ASR gate remains open.

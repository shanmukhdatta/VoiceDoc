# Bilingual voice pipeline baseline

- English/Telugu raw text normalization is loss-minimizing; mixed Telugu/Latin code-switch text is detected but not blindly translated or rewritten. Negations and quantities remain intact.
- Basic energy-based voice activity detector segments 16 kHz mono PCM without native dependencies; **not Silero and not a reliable ASR quality substitute**. Production Silero ONNX remains a gated model integration.
- Android API 31+ device-only speech recognition can insert a transcript into Chat. It cannot use Android's network-backed fallback and does not automatically submit the potentially erroneous transcript.
- Chat may speak ONLY the validated static offline fallback via an installed English/Telugu Android voice reporting no network required. If no voice exists, speech remains unavailable. Always test actual audio in airplane mode.
- Developer diagnostics use Android ActivityManager/Debug for actual RAM and process PSS.
- A real Whisper multilingual model, robust Telugu accent/code-switch ASR, trusted sources, E5 ONNX and grounded Gemma generation still remain unimplemented, and must be verified separately.

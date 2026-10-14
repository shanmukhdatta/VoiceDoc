# Runtime experimental model and speech support

This branch adds a real LiteRT-LM 0.18.0 Android dependency and Kotlin `Engine` CPU adapter, plus Android 12+ on-device-only SpeechRecognizer. Neither requires an internet permission to perform inference. App requests mic permission only on use; it does not fallback to Android's possibly network-backed regular SpeechRecognizer.

The local model import from Android's document picker copies a developer-supplied `.litertlm` candidate into no-backup internal storage, with SHA-256 checksum and 1.5 GB cap. Imported checksum is **NOT authentic source verification**; check the published artifact SHA/license separately. The model is **never** used for health chat, and the only prompt is a benign fixed greeting benchmark. App CPU/ABI support is device-specific. Valid upstream LiteRT artifact and Gemma license acceptance are required before testing.

Official API: https://developers.google.com/edge/litert-lm/android
Android on-device ASR: https://developer.android.com/reference/android/speech/SpeechRecognizer

**Not completed:** multilingual Whisper JNI, approved release-grade model manifest signature, real Telugu health ASR quality, safety-approved LLM generation. These remain blocked, not mocked.

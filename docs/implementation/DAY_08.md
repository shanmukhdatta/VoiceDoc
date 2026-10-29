# Day 08 — Multilingual processing, offline TTS and complete voice conversation

**Date:** 2026-11-02  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Continue implementing the [full 40-section README](../../README.md).  
**Outcome:** Join real ASR, language processing, local evidence retrieval, SLM generation, safety checks and offline spoken playback, verifying each language independently.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. Leave a checkbox unchecked until the stated action is actually verified. Day numbers represent the intended order, not permission to skip unfinished safety-critical work. **Never claim clinical reliability or published availability without validation.**

## Task A — Learn before coding

- [ ] **01.** Read Unicode normalization, language codes en/te/ta, script differences and code-switching.
- [ ] **02.** Differentiate auto language detection from manual selection; preserve medically relevant numbers and negation.
- [ ] **03.** Study offline TextToSpeech, sherpa-onnx voice packages, sentence segmentation and playback.
- [ ] **04.** Understand Android audio focus, accessibility labels and audio interruptions.
- [ ] **05.** Learn language-by-language transcript, retrieval, answer-language and TTS evaluation methods.

## Task B — Setup and environment

- [ ] **01.** Keep the existing English/Telugu selection functional while gradually adding Tamil.
- [ ] **02.** Inspect actual device voice availability with networking disabled; record supported locales and engine IDs.
- [ ] **03.** Check licenses/sizes for any local TTS voice/model packs and obtain legitimate files.
- [ ] **04.** Build safe language fixtures with medical terminology, measurements, transliteration and code-switching.
- [ ] **05.** Calculate total storage budget for ASR, embedding model, SLM, RAG index and TTS.

## Task C — Small isolated exercise

- [ ] **01.** Normalize sample English, Telugu and Tamil text without altering numbers/units.
- [ ] **02.** Make a small offline text-to-speech playback UI and confirm it speaks in airplane mode.
- [ ] **03.** Build a stop/replay/pause (where supported) voice interaction exercise.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Implement LanguageProcessor normalization and optional language detection with explicit override.
- [ ] **02.** Preserve original transcript, normalized retrieval query and preferred response language separately.
- [ ] **03.** Evaluate multilingual embedding retrieval and mark languages unverified if results are weak.
- [ ] **04.** Implement TextToSpeechEngine behind a replaceable interface (local Android voice or sherpa-onnx).
- [ ] **05.** Handle missing/offline-unavailable TTS voices by falling back to on-screen text.
- [ ] **06.** Implement source-safe sentence segmentation, speak/stop/replay and audio focus handling.
- [ ] **07.** Feed ONLY approved final response content to TTS, never raw unchecked tokens.
- [ ] **08.** Wire microphone → VAD → ASR → normalization → safety → embeddings/RAG → prompt → SLM → safety → text/TTS.
- [ ] **09.** Add correction/retry, mute, text-only and voice-only interaction choices where accessible.
- [ ] **10.** Test state transitions for repeated turns, cancellations and interrupted audio.
- [ ] **11.** Add localization for high-priority UI messages while labelling untranslated content.
- [ ] **12.** Record end-to-end latency and language-specific component performance.

## Task E — Experiments / learn by comparison

- [ ] **01.** Test airplane-mode flow for English, Telugu and Tamil separately.
- [ ] **02.** Test code-switched phrases and contrast retrieval before/after transcript correction.
- [ ] **03.** Compare voice engines on speech intelligibility, memory footprint and offline availability.
- [ ] **04.** Test repeated TTS plays, Bluetooth/headphones (if available), audio focus interruptions and stop mid-sentence.

## Task F — Testing and verification

- [ ] **01.** Only language/ASR/TTS combinations actually verified appear as supported.
- [ ] **02.** A selected verified language completes an offline voice-to-answer cycle.
- [ ] **03.** Unavailable voice yields readable text, not a crash.
- [ ] **04.** Output safety policy is enforced before voice playback.
- [ ] **05.** No microphone or player leaks after cancellations or navigation.

## Task G — Proof of completion

- [ ] **01.** Multilingual language adapter, TTS engine and full offline voice path.
- [ ] **02.** Language capability matrix with supported/limited/unverified states and measured accuracy.
- [ ] **03.** Screenshots/recordings of permitted demo fixtures, performance results and commit SHA.
- [ ] Record device/OS specs, model/dependency versions, commands and results in `docs/learning/day-08-notes.md`.
- [ ] Explain in your own words what you built, the logic, assumptions, limits and what remains broken.
- [ ] Inspect `git diff` and `git status`, run tests and commit only verified changes as `day 08: <actual completed work>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) based on evidence.

## Exit criteria (mandatory)

At least one full voice-to-safe-response-to-voice path passes on the test phone; each other language is separately verified or explicitly blocked.

## Official learning references

- [sherpa-onnx project](https://github.com/k2-fsa/sherpa-onnx)
- [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [Android accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing)

## When blocked

1. Keep the exact failing command, stack trace, hardware/software info and expected result.
2. Ask AI for an explanation plus one hint, then attempt the change yourself.
3. Create a minimal reproduction, verify the fix, rerun tests and record the outcome.
4. If resources or data aren't available, label the affected task **BLOCKED** with evidence; continue independent preparation, but do not count a mock as a real subsystem.

---

**Next:** [Day 09](DAY_09.md)

# Day 07 — Voice activity detection and offline ASR with whisper.cpp

**Date:** 2026-11-01  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Continue implementing the [full 40-section README](../../README.md).  
**Outcome:** Speak into the phone, get a genuine local transcript and feed the previously working text pipeline.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. Leave a checkbox unchecked until the stated action is actually verified. Day numbers represent the intended order, not permission to skip unfinished safety-critical work. **Never claim clinical reliability or published availability without validation.**

## Task A — Learn before coding

- [ ] **01.** Study microphone buffers, PCM16, mono audio, sample rate and resampling.
- [ ] **02.** Understand VAD window sizes, thresholds, false triggers and word truncation risks.
- [ ] **03.** Read Android NDK, JNI, CMake and native library lifecycle basics.
- [ ] **04.** Study whisper.cpp official Android examples, multilingual model sizes and licenses.
- [ ] **05.** Learn word error rate (WER), character error rate (CER), medical-term accuracy and real-time factor.

## Task B — Setup and environment

- [ ] **01.** Confirm Day 2 real microphone capture works and Day 6 typed pipeline is tested.
- [ ] **02.** Install Android NDK and CMake; document actual versions and supported ABIs.
- [ ] **03.** Read official whisper.cpp Android sample and license before adding native dependency.
- [ ] **04.** Download/prepare one compatible multilingual checkpoint outside Git; record exact source, hash, size and usage terms.
- [ ] **05.** Prepare a small consented or self-recorded test set of English, Telugu, Tamil and code-switching; no real patient audio.

## Task C — Small isolated exercise

- [ ] **01.** Record and inspect a WAV file's channels/sample rate then transcribe it locally with the official sample.
- [ ] **02.** Write an RMS-based audio-level meter; compare silence and speech frames.
- [ ] **03.** Experiment with too-aggressive and moderate VAD trimming and listen for missing words.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Implement reusable VAD interface, speech buffer, timeouts and maximum capture duration.
- [ ] **02.** Preserve enough pre/post-speech padding to avoid losing the beginning and end of words.
- [ ] **03.** Add JNI/CMake bridge for whisper.cpp under an ASR module without polluting the UI layer.
- [ ] **04.** Implement model initialization, transcription, cancellation and release behind SpeechRecognizerEngine.
- [ ] **05.** Validate checkpoint/ABI mismatch, unavailable native library and corrupted model failures.
- [ ] **06.** Connect AudioRecord → VAD → ASR → selected-language validation → text normalization.
- [ ] **07.** Add editable transcript confirmation before sending it to the medical-answer pipeline.
- [ ] **08.** Support microphone errors, denied permission, noisy/empty audio and interruption.
- [ ] **09.** Record ASR latency and memory/performance without logging raw voice/transcript by default.
- [ ] **10.** Wire transcript output to the same orchestrator used by typed questions.
- [ ] **11.** Write tests for VAD buffering, audio data shape, stage cancellation and transcript review.
- [ ] **12.** Run a real offline spoken-query test after all required models are installed.

## Task E — Experiments / learn by comparison

- [ ] **01.** Benchmark tiny vs base checkpoint if device resources allow; record model size and WER/CER.
- [ ] **02.** Repeat utterances in quiet and noisy environments for three languages.
- [ ] **03.** Test 1s, 15s and longer speech and several start/stop cycles.
- [ ] **04.** Compare edited and unedited transcript impact on subsequent retrieval.

## Task F — Testing and verification

- [ ] **01.** A real locally recorded utterance produces real on-device ASR output.
- [ ] **02.** Transcript can be corrected before downstream health-answer generation.
- [ ] **03.** Missing model or native binary fails with a clear message, not invented text.
- [ ] **04.** No audio uploads; microphone released after stop, cancel, backgrounding or error.
- [ ] **05.** Language-specific accuracy is measured; not asserted from a single demo.

## Task G — Proof of completion

- [ ] **01.** Offline VAD/whisper.cpp integration plus editable transcript UI.
- [ ] **02.** Device ASR benchmark report with WER/CER and measured language support.
- [ ] **03.** Checkpoint license/hash manifest, integration tests and Git commit SHA.
- [ ] Record device/OS specs, model/dependency versions, commands and results in `docs/learning/day-07-notes.md`.
- [ ] Explain in your own words what you built, the logic, assumptions, limits and what remains broken.
- [ ] Inspect `git diff` and `git status`, run tests and commit only verified changes as `day 07: <actual completed work>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) based on evidence.

## Exit criteria (mandatory)

A physical Android phone converts speech to a transcript in airplane mode with real model inference and predictable errors.

## Official learning references

- [whisper.cpp Android example](https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android)
- [Android NDK guides](https://developer.android.com/ndk/guides)
- [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)

## When blocked

1. Keep the exact failing command, stack trace, hardware/software info and expected result.
2. Ask AI for an explanation plus one hint, then attempt the change yourself.
3. Create a minimal reproduction, verify the fix, rerun tests and record the outcome.
4. If resources or data aren't available, label the affected task **BLOCKED** with evidence; continue independent preparation, but do not count a mock as a real subsystem.

---

**Next:** [Day 08](DAY_08.md)

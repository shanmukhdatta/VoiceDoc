# Day 02 — Interactive chat, state, persistence and raw microphone audio

**Date:** 2026-10-27  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Implement the [40-section README](../../README.md) gradually, not a fake demo.  
**Outcome:** Provide a truly interactive but explicitly model-disconnected interface, plus recorded PCM input and privacy-conscious settings.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. The checkboxes are your personal tracker; leave unchecked until a real result exists. Learning hours and tests may extend past the calendar day; never mark unverified work complete.

## Task A — Learn before coding

- [ ] **01.** Study coroutines, suspend functions, Dispatchers, Flow, StateFlow and cancellation.
- [ ] **02.** Understand MVVM: UI observes immutable UiState and emits user intents.
- [ ] **03.** Read Room entities/DAO/migrations and DataStore preferences.
- [ ] **04.** Read runtime microphone permission, AudioRecord and foreground/background capture constraints.
- [ ] **05.** Understand WAV headers, mono PCM, sample rate, amplitude and avoiding recording on the main thread.

## Task B — Setup and environment

- [ ] **01.** Confirm Day 1 build and git status are clean.
- [ ] **02.** Add compatible lifecycle/ViewModel, coroutines, navigation, Room and DataStore dependencies with pinned versions; inspect Gradle sync.
- [ ] **03.** Request RECORD_AUDIO permission only when the user presses Record; confirm permission-denied flow.
- [ ] **04.** Choose a local app-private location for temporary PCM/WAV; disable media auto-upload and never add raw recordings to Git.

## Task C — Small isolated exercise

- [ ] **01.** Build a tiny coroutine counter or timer with start/cancel and StateFlow.
- [ ] **02.** Record five seconds of mono audio and replay it locally; verify actual sample rate and PCM format.
- [ ] **03.** Create and delete one Room row using a DAO unit test.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Create ChatUiState with idle, listening, transcribing, retrieving, generating, speaking, cancelled and failed substates.
- [ ] **02.** Implement text-input send/edit, send button, scrollable messages, cancellation and nonblocking progress indicator.
- [ ] **03.** Use an explicitly labelled FakeLanguageModelEngine *for UI wiring only*; no mock answer may masquerade as a health answer.
- [ ] **04.** Design a data access layer for settings (language, voice, consent/history, developer mode) using DataStore.
- [ ] **05.** Define optional history persistence disabled by default; separate transient conversation context from trusted medical data.
- [ ] **06.** Implement Room schema for permitted history and metadata; migrations and repository tests.
- [ ] **07.** Implement audio capture (16 kHz mono PCM only if supported; resample carefully where needed), start/stop/cancel and clean resource release.
- [ ] **08.** Render live mic amplitude/time, audio permission denial, no mic device, timeout and interruption states.
- [ ] **09.** Implement clear-history and clear-recording actions; never store full audio by default.
- [ ] **10.** Write small tests for ViewModel state transitions, cancellation and persistence opt-in.
- [ ] **11.** Review every UI string that might suggest a diagnosis; include health-information-only boundary.

## Task E — Experiments / learn by comparison

- [ ] **01.** Record silence, normal speech, loud speech and a noisy sample; compare wave durations and amplitude.
- [ ] **02.** Put phone in airplane mode and verify chat UI and recording do not require network.
- [ ] **03.** Force deny/revoke microphone permission; ensure the app remains usable for typed input.

## Task F — Testing and verification

- [ ] **01.** Typed input displays properly but does not claim a real AI response.
- [ ] **02.** Recorded PCM/WAV can be inspected and deleted.
- [ ] **03.** No UI freeze while recording, storing or cancelling.
- [ ] **04.** History remains off until enabled and deletion actually removes stored state.
- [ ] **05.** A failed recorder or missing model yields explicit error messages.

## Task G — Proof of completion

- [ ] **01.** Functional Chat + Settings screens, audio capture adapter and Room/DataStore persistence.
- [ ] **02.** Tests for UI state, database, permissions and error paths.
- [ ] **03.** Sample WAV (local only), screenshots, device log, commit SHA.
- [ ] Record exact versions, device/OS specs, commands run and logs in `docs/learning/day-02-notes.md`.
- [ ] Write three sentences in your own words: what you built, how it works, and what remains broken.
- [ ] Run `git status`, review your diff, run relevant tests, then commit with `day 02: <verified result>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) accurately.

## Exit criteria (mandatory)

Day 2 passes when user input, recording and local persistence behave correctly without any network or fake clinical answer.

## Official learning references

- [Android coroutines](https://developer.android.com/kotlin/coroutines)
- [Room persistence library](https://developer.android.com/training/data-storage/room)
- [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

## When blocked

1. Preserve the failing command, console output, device/runtime details and the expected result.
2. Ask AI to explain the error and give **one hint**; try your own fix first.
3. Write a minimal reproduction, apply a verified fix, rerun tests and document the result.
4. If model/hardware/data access is impossible, mark **BLOCKED** with evidence and use a clearly labelled interface-level fallback for development; do **not** claim the real feature works.

---

**Next:** [Day 03](DAY_03.md).

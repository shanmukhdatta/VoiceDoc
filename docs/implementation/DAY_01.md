# Day 01 — Understand the repository, Kotlin, Compose and clean architecture

**Date:** 2026-10-26  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Implement the [40-section README](../../README.md) gradually, not a fake demo.  
**Outcome:** Run the existing foundation, map every planned component, and make an extensible navigable Android shell without rewriting working code.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. The checkboxes are your personal tracker; leave unchecked until a real result exists. Learning hours and tests may extend past the calendar day; never mark unverified work complete.

## Task A — Learn before coding

- [ ] **01.** Read Kotlin variables, null safety, functions, classes, interfaces, sealed types and data classes; write 2 examples of each.
- [ ] **02.** Complete the beginner Compose sections on composables, remember/rememberSaveable, events and state hoisting.
- [ ] **03.** Explain Activity, Composable, ViewModel, domain layer and repository in your own notes.
- [ ] **04.** Read the existing README sections 1-5, 20-22, 31-33 and inspect settings.gradle.kts and app/build.gradle.kts.
- [ ] **05.** Learn git status, branch, add, commit, diff and how to revert a mistaken local change.

## Task B — Setup and environment

- [ ] **01.** Clone/pull main; run `git status`; ensure a clean baseline; never overwrite uncommitted work.
- [ ] **02.** Open the existing `android/` Gradle project in Android Studio; verify JDK 17 and installed SDK 36 (matching checked project configuration), emulator or physical Android phone.
- [ ] **03.** Enable USB debugging if using a phone; record Android version, chip, RAM, storage and available GPU/NPU capabilities.
- [ ] **04.** Run `./gradlew :app:assembleDebug` from `android/` (Windows: `gradlew.bat :app:assembleDebug`); capture full error text if it fails.
- [ ] **05.** Run existing app and record a screenshot of the English/Telugu foundation UI; note its current limitations honestly.

## Task C — Small isolated exercise

- [ ] **01.** In a disposable Kotlin playground, implement a `Result` sealed interface with Success/Failure.
- [ ] **02.** Create a Compose counter screen with two buttons and saved state.
- [ ] **03.** Explain in `docs/learning/day-01-notes.md` why business logic does not belong in MainActivity.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Map README scope to module ownership (audio/asr/language/embedding/rag/llm/safety/tts/data/model-manager/metrics).
- [ ] **02.** Define pure domain types: ConversationTurn, Transcript, RetrievedChunk, EvidenceReference, ModelStatus, SafetyDecision, PipelineError.
- [ ] **03.** Define interfaces for SpeechRecognizerEngine, LanguageProcessor, EmbeddingEngine, Retriever, PromptBuilder, LanguageModelEngine, SafetyEngine, TextToSpeechEngine, ModelManager and MetricsCollector.
- [ ] **04.** Create only the Gradle modules that can compile today; add others gradually when implementations need them.
- [ ] **05.** Replace one-screen-only UI with Chat, Models, Settings, Knowledge/Sources and Developer Metrics screen navigation.
- [ ] **06.** Add default empty/loading/error placeholders that clearly say unavailable rather than falsely implying inference works.
- [ ] **07.** Keep existing English/Telugu text as-is; add Tamil as a *planned/unverified* language option rather than claiming it works.
- [ ] **08.** Keep INTERNET permission absent in inference-only builds unless a strictly optional download path is deliberately designed.
- [ ] **09.** Add a basic unit test for language enum/normalization and one Compose UI smoke test.
- [ ] **10.** Build and run the app again; commit only after tests pass.

## Task E — Experiments / learn by comparison

- [ ] **01.** Rotate the phone and verify selected screen and user-edited text behave as intended.
- [ ] **02.** Force a failed or missing model status and verify the UI reports it honestly.
- [ ] **03.** Write down whether each intended module is domain code, Android infrastructure or UI.

## Task F — Testing and verification

- [ ] **01.** `./gradlew :app:assembleDebug` exits successfully.
- [ ] **02.** Navigation reaches all five screens without crashing.
- [ ] **03.** Existing English and Telugu selection still works.
- [ ] **04.** No AI output, permissions or medical functionality is claimed prematurely.
- [ ] **05.** No hard-coded personal data, tokens, models or restricted datasets enter Git.

## Task G — Proof of completion

- [ ] **01.** `docs/learning/day-01-notes.md` (you create when implementing).
- [ ] **02.** Compiling modular app shell, domain contracts and unit/UI tests.
- [ ] **03.** A screenshot, build log, phone-spec note and the day's Git commit SHA.
- [ ] Record exact versions, device/OS specs, commands run and logs in `docs/learning/day-01-notes.md`.
- [ ] Write three sentences in your own words: what you built, how it works, and what remains broken.
- [ ] Run `git status`, review your diff, run relevant tests, then commit with `day 01: <verified result>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) accurately.

## Exit criteria (mandatory)

Move on only if the foundation builds, all intended screens open, contracts compile and the prior working interface is preserved.

## Official learning references

- [Android Basics with Compose](https://developer.android.com/courses/android-basics-compose/course)
- [Guide to Android app architecture](https://developer.android.com/topic/architecture)
- [Kotlin official basics](https://kotlinlang.org/docs/basic-syntax.html)

## When blocked

1. Preserve the failing command, console output, device/runtime details and the expected result.
2. Ask AI to explain the error and give **one hint**; try your own fix first.
3. Write a minimal reproduction, apply a verified fix, rerun tests and document the result.
4. If model/hardware/data access is impossible, mark **BLOCKED** with evidence and use a clearly labelled interface-level fallback for development; do **not** claim the real feature works.

---

**Next:** [Day 02](DAY_02.md).

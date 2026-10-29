# Day 10 — Model packs, reliability, benchmarking, deployment and public prototype release

**Date:** 2026-11-04  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Continue implementing the [full 40-section README](../../README.md).  
**Outcome:** Integrate the entire system, run acceptance tests in airplane mode, produce a signed installable package and prepare an honest public research demo.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. Leave a checkbox unchecked until the stated action is actually verified. Day numbers represent the intended order, not permission to skip unfinished safety-critical work. **Never claim clinical reliability or published availability without validation.**

## Task A — Learn before coding

- [ ] **01.** Read Android build variants, signed APK vs Play Store AAB, target/min SDK and release keys.
- [ ] **02.** Understand app-private model packs, manifests, checksums, update/migration strategies and constrained storage.
- [ ] **03.** Read instrumentation/Compose UI tests, unit tests, integration tests and benchmark profiling.
- [ ] **04.** Study CI with GitHub Actions, reproducible release artifacts and SHA-256 checksums.
- [ ] **05.** Read relevant Google Play developer policy about health claims/privacy; publication approval is separate from technical readiness.

## Task B — Setup and environment

- [ ] **01.** Freeze versions, Git commit SHA, model/index manifest and supported Android device matrix.
- [ ] **02.** Confirm device has enough free RAM/storage for ALL simultaneously installed models.
- [ ] **03.** Create privacy policy, medical-information disclaimer, limitations, model/data licenses, security notes and contact pathway.
- [ ] **04.** Determine distribution: GitHub Releases signed APK, optionally Play Store AAB when account/policies are ready.
- [ ] **05.** Make signing keystore locally and store securely OUTSIDE Git; never commit secret keys, passwords or personal recordings.

## Task C — Small isolated exercise

- [ ] **01.** Write a SHA-256 validator for a model pack and deliberately corrupt the file to test rejection.
- [ ] **02.** Create a tiny performance chart from real local timing logs.
- [ ] **03.** Build a signed sample APK and verify that signature/installation works on physical Android.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Implement model-pack install/import/status/uninstall interfaces; verify hash, version, available space and compatibility before loading.
- [ ] **02.** Build robust model-corrupt/missing/incompatible errors, rollback and safe fallback handling.
- [ ] **03.** Complete local developer metrics screen (ASR, embedding, retrieval, TTFT, tokens/s, TTS, total latency, RAM, thermal).
- [ ] **04.** Integrate all final chat, settings, model management, knowledge/source and metrics UI screens.
- [ ] **05.** Create full offline end-to-end verification script and manual test checklist.
- [ ] **06.** Create unit/instrumentation/integration tests for every component and negative/error path.
- [ ] **07.** Run language-specific ASR, RAG, generation, safety and TTS evaluation; record measured results.
- [ ] **08.** Run repeated-conversation endurance tests and app restart/crash/recovery tests.
- [ ] **09.** Test local history consent, deletion, backups, permission handling and no unwanted network activity.
- [ ] **10.** Add GitHub Actions CI for lint/unit tests and APK build where compatible; avoid unsafe artifact disclosure.
- [ ] **11.** Build signed release APK (and AAB if publishing to Play Store); verify clean-device installation.
- [ ] **12.** Prepare GitHub Releases with APK, checksums, system requirements, model asset instructions and known limitations.
- [ ] **13.** Prepare README getting-started, architecture, evaluation summary, supported-language/backend matrix, demo assets and screenshots.
- [ ] **14.** Confirm bundled/linked model, font, dataset, TTS and guideline redistribution permissions before releasing packages.
- [ ] **15.** If pursuing Google Play, verify developer eligibility, applicable health-app rules, Data safety form, privacy policy and review requirements; submit only after compliance review.
- [ ] **16.** Document honest remaining issues and next experiments; do not call a merely compiling component 'implemented and verified'.

## Task E — Experiments / learn by comparison

- [ ] **01.** Run complete pipeline with airplane mode enabled after local model provisioning, restarting app first.
- [ ] **02.** Compare cold/warm start times, several context lengths, and repeated sessions on the same hardware.
- [ ] **03.** Test wrong/corrupt model, full storage, low memory, missing mic, no TTS voice and index mismatch.
- [ ] **04.** Ask a consenting tester to try a nonclinical demo script and capture UX problems without retaining sensitive data.

## Task F — Testing and verification

- [ ] **01.** A real complete supported voice → transcript → local sources → SLM → safe text → offline TTS demo succeeds.
- [ ] **02.** All claimed supported languages have recorded per-component evidence; others are visibly limited.
- [ ] **03.** App and bundled/installed models run without Internet at inference.
- [ ] **04.** Release APK is signed, installs on target device and launches without debug-only requirements.
- [ ] **05.** CI results, checksums, security/privacy tests and model licenses are documented.
- [ ] **06.** Distribution page does not present the app as a diagnosis or treatment system.

## Task G — Proof of completion

- [ ] **01.** Signed distributable APK + SHA-256 and installation instructions (if build actually succeeds).
- [ ] **02.** Updated README, model/data cards, privacy/disclaimer files, CI, known-issues and release notes.
- [ ] **03.** Full tests/benchmarks, actual offline demo evidence, commit SHA and publication decision.
- [ ] Record device/OS specs, model/dependency versions, commands and results in `docs/learning/day-10-notes.md`.
- [ ] Explain in your own words what you built, the logic, assumptions, limits and what remains broken.
- [ ] Inspect `git diff` and `git status`, run tests and commit only verified changes as `day 10: <actual completed work>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) based on evidence.

## Exit criteria (mandatory)

Prototype completion requires a real repeatable offline end-to-end device demo; public Play Store approval and clinical validation are separate milestones.

## Official learning references

- [Android prepare for release](https://developer.android.com/studio/publish/preparing)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
- [Android Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
- [Google Play health content guidance](https://support.google.com/googleplay/android-developer/answer/9888170)
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)

## When blocked

1. Keep the exact failing command, stack trace, hardware/software info and expected result.
2. Ask AI for an explanation plus one hint, then attempt the change yourself.
3. Create a minimal reproduction, verify the fix, rerun tests and record the outcome.
4. If resources or data aren't available, label the affected task **BLOCKED** with evidence; continue independent preparation, but do not count a mock as a real subsystem.

---

**Next:** Review the master [release readiness checklist](../../IMPLEMENTATION_PLAN.md) and remaining blockers.

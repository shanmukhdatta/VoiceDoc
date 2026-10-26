# Day 06 — Medical safety, evidence gating, orchestration and privacy

**Date:** 2026-10-31  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Continue implementing the [full 40-section README](../../README.md).  
**Outcome:** Convert the text pipeline into a controlled research assistant: grounded sources, explicit uncertainty, input/output guardrails and private state.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. Leave a checkbox unchecked until the stated action is actually verified. Day numbers represent the intended order, not permission to skip unfinished safety-critical work. **Never claim clinical reliability or published availability without validation.**

## Task A — Learn before coding

- [ ] **01.** Read README sections 2, 11, 18, 24-26, 30-33 and write what the app must NOT claim.
- [ ] **02.** Distinguish input intent checks, retrieval confidence checks, output grounding checks and escalation policies.
- [ ] **03.** Learn deterministic rules, structured response states, false reassurance and false-positive fallback.
- [ ] **04.** Study Android Keystore, storage encryption, backup exclusion and protecting sensitive logs.
- [ ] **05.** Read about prompt injection: retrieved source text is evidence, not an instruction to the model.

## Task B — Setup and environment

- [ ] **01.** Create folders for safety policies, rule definitions, test cases and evaluation reports.
- [ ] **02.** Define decision states: ANSWER_WITH_SOURCES, NEED_MORE_INFO, NOT_ENOUGH_EVIDENCE, URGENT_ESCALATION, OUT_OF_SCOPE, ERROR.
- [ ] **03.** Prepare nonpersonal test queries with explicitly labelled expected outcomes; do not use real patient records.
- [ ] **04.** Identify a reputable clinical reviewer/clinical guidance source for escalation wording; document pending reviews honestly.
- [ ] **05.** Verify that history consent is OFF by default and sensitive local data is excluded from backups.

## Task C — Small isolated exercise

- [ ] **01.** Build a tiny nonmedical policy-rule engine with PASS, BLOCK and UNCERTAIN states.
- [ ] **02.** Create unit tests for the rule engine and print the rule ID that caused a decision.
- [ ] **03.** Put an 'ignore previous instructions' phrase inside a sample document and ensure it is treated as untrusted text.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Implement typed InputSafetyEngine and OutputSafetyEngine interfaces; tests must not depend on a real LLM.
- [ ] **02.** Implement deterministic urgency/out-of-scope detectors and vetted fallbacks; document coverage gaps.
- [ ] **03.** Add retrieval confidence and source-provenance validation before prompting.
- [ ] **04.** Build a PromptBuilder with source delimiters, language, current query, short permitted history, max-context budget and safety policy.
- [ ] **05.** Add grounding verification for each generated citation ID: it must exist in the retrieved source set.
- [ ] **06.** Buffer or validate model output before ordinary display and TTS; do not expose unchecked medical generation as approved advice.
- [ ] **07.** Create end-to-end VoiceDoctorOrchestrator wiring safety → retrieval → prompt → LLM → output safety.
- [ ] **08.** Implement typed stage errors, timeouts, cancellation, retry policy and user-visible failure messages.
- [ ] **09.** Add opt-in history retention, encrypted sensitive storage, clear-history action and conservative retention defaults.
- [ ] **10.** Keep raw health question/audio out of logs and telemetry; document secure debug-mode rules.
- [ ] **11.** Add source metadata display and a 'not a diagnostic service' product boundary notice.
- [ ] **12.** Build synthetic safety regression tests covering unsupported answers, urgent wording, missing sources and adversarial content.

## Task E — Experiments / learn by comparison

- [ ] **01.** Compare outcomes with good evidence versus zero evidence and conflicting evidence.
- [ ] **02.** Run at least 30 labelled safety scenarios; measure missed escalations, false reassurance and inappropriate refusals.
- [ ] **03.** Test malformed citation IDs and missing source metadata in generated output.
- [ ] **04.** Inspect Logcat and Android backup configuration to verify data-minimization defaults.

## Task F — Testing and verification

- [ ] **01.** Unsafe or unsupported responses do not appear as trusted results or reach TTS.
- [ ] **02.** No-evidence flow produces explicit uncertainty rather than fake medical citations.
- [ ] **03.** Cancellation mid-pipeline leaves app responsive and resources cleaned up.
- [ ] **04.** History must remain off until opted in; delete-history removes persisted records.
- [ ] **05.** Typed retrieval-to-answer path works in airplane mode without cloud calls.

## Task G — Proof of completion

- [ ] **01.** Versioned policies, engine interfaces/implementations, orchestrator and unit tests.
- [ ] **02.** Threat model, privacy documentation, safe fallback catalogue and test evaluation report.
- [ ] **03.** Actual logs/screenshots, reproducible commands, pass/fail evidence and Git commit SHA.
- [ ] Record device/OS specs, model/dependency versions, commands and results in `docs/learning/day-06-notes.md`.
- [ ] Explain in your own words what you built, the logic, assumptions, limits and what remains broken.
- [ ] Inspect `git diff` and `git status`, run tests and commit only verified changes as `day 06: <actual completed work>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) based on evidence.

## Exit criteria (mandatory)

The verified local typed pipeline cannot bypass the tested safety gates. Clinical reliability is NOT established by code tests alone.

## Official learning references

- [Android security](https://developer.android.com/privacy-and-security)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [OWASP LLM security](https://genai.owasp.org/)

## When blocked

1. Keep the exact failing command, stack trace, hardware/software info and expected result.
2. Ask AI for an explanation plus one hint, then attempt the change yourself.
3. Create a minimal reproduction, verify the fix, rerun tests and record the outcome.
4. If resources or data aren't available, label the affected task **BLOCKED** with evidence; continue independent preparation, but do not count a mock as a real subsystem.

---

**Next:** [Day 07](DAY_07.md)

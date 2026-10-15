# Phase 02 — Offline pipeline contracts

**Status: IMPLEMENTED IN SOURCE; BUILD AND DEVICE VALIDATION PENDING**

This phase introduces pure Kotlin contracts for offline retrieval, generation, evidence provenance and pre-generation urgency routing, plus unit tests.

## Deliberate limitations
- No ASR, TTS, embedding model, vector store, LLM binary or Android wiring is claimed.
- The urgency gate is an interface, **not** a medically validated symptom detector. Until validated, it must not be treated as one.
- Generated text is **not** a medically validated or post-checked recommendation. Production releases require a reviewed output policy, multilingual urgent-case coverage, clinician-reviewed content, a verified licensed corpus and evaluation.
- Model missing, invalid question and missing verified evidence have explicit non-generative statuses.
- No remote API is required by these contracts.

## Next integration gates
1. Implement vetted multilingual urgency rules and clinician-backed evaluation datasets.
2. Add local curated corpus with provenance and license review, normalized embedding extraction and exact top-k retrieval.
3. Integrate offline ASR and quantized LLM with device-tested runtimes.
4. Add output groundedness checks and refusal logic before UI display/TTS.
5. Add persistent private settings, opt-in encrypted history, accessibility, and offline-language tests.
6. Confirm on-device memory, runtime, language and safety benchmarks in Antigravity, then review PR.

Unit tests are added to the existing :core:common:testDebugUnitTest target.

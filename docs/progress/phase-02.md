# Voice Doctor AI Phase 02 — Android Application Foundation
**STATUS: IMPLEMENTED IN CODE, CI AND EMULATOR VERIFICATION REQUIRED.**
Branch: `phase-02/app-experience`

## Included
- Fully navigable native Jetpack Compose foundation: onboarding, dashboard, text chat (explicit conservative no-medical-model fallback), knowledge source status, model status, settings, privacy, ephemeral history status, diagnostics, about.
- English/Telugu interface and persisted non-sensitive language/onboarding preferences.
- Ephemeral text chat only; clearing session supported. **No stored medical conversation history**, remote calls, automatic model downloads, fake citations, fake clinical response or microphone permission.
- Conservative independent keyword warnings for examples of urgent concerns **not validated clinical triage**. All other medical questions abstain until verified KB/SLM exists.
- Unit tests for language handling and fallback. Phase 01 privacy instrumentation preserved.

## Acceptance
- [ ] GitHub CI `:core:common:testDebugUnitTest :app:assembleDebug` success
- [ ] Local Android emulator install/launch, navigation screens, English/Telugu copy, rotation, session clear and onboarding reset
- [ ] Instrumentation test on emulator and no INTERNET permission in merged APK
- [ ] Accessibility/layout checks on small screen
- [ ] No clinical claims or fabricated answers

## Still required in other phases
- Genuine speech ASR/VAD and offline TTS.
- Licensed medical KB and bilingual provenance reviewed by clinician.
- Multilingual embeddings/hybrid retrieval with real data.
- Real quantized SLM (LiteRT-LM) with hardware and quality evaluation.
- Independent clinician-validated safety engine; existing keyword fallback is **not sufficient**.
- Opt-in encrypted history and trusted model manager.
- Benchmarks, conversion/fine-tuning scripts, final distribution signing.

Phase 00 remains awaiting physical hardware/Telugu TTS validation.

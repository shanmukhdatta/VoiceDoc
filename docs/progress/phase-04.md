# Phase 04 — Typed-question readiness UI

**Status: SOURCE PUSHED; CI and Android emulator validation pending.**

Adds a local typed-question entry form for English and Telugu and an honest readiness status. Clears text without sending it to a remote service. Does not store questions on disk.

This is **not** a working medical assistant. It intentionally avoids calling an unvalidated safety gate, inference model or unreviewed knowledge pack. Voice capture, microphone permission, speech recognition, retrieval, model inference and speech output must remain disabled until independently implemented and evaluated.

## Run checks
`cd android && ./gradlew :core:common:testDebugUnitTest :app:assembleDebug`

On Antigravity, confirm: language toggle, empty-input disabled state, max 2000 characters, status response, Clear, rotation, offline mode, no INTERNET/RECORD_AUDIO permission and no health output.

This PR is stacked on Phase 03. Merge upstream PRs in order and retarget as needed.

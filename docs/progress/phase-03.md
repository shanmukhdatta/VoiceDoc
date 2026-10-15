# Phase 03 — Local evidence retrieval baseline

**Status: SOURCE PUSHED; UNIT TESTS AND CI PENDING.**

Adds a pure Kotlin zero-network lexical retriever with Unicode tokenization, language filtering, deterministic ranking, relevance threshold, and source metadata preservation.

There is intentionally **no bundled health knowledge**. This implementation does not invent medical citations, distribute unlicensed documents or claim the presence of a medically reviewed corpus.

### Acceptance requirements for use in a medical app
- Add source manifests, rights/attribution verification, approved publication editions, document hashes, and a reviewed multilingual medical corpus.
- Replace or supplement lexical indexing with an evaluated multilingual embedding index and inspect false-match/low-relevance failure modes.
- Test normalization for code-mixed Telugu/Tamil/English and medical terms.
- Keep metadata filters and explicit no-evidence responses.
- Do not equate `verified=true` (source-pack admission) with clinical correctness; record independent medical reviewer sign-off.
- Test resources and phones in airplane mode; record latency, RAM and battery usage.

### Run
`cd android && ./gradlew :core:common:testDebugUnitTest :app:assembleDebug`

The branch is stacked on Phase 02 so it can be reviewed while the prior phase's PR is open. Merge Phase 02 first, then retarget this PR to main before merging.

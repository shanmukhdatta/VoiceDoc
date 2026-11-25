# Phase 04/05 — Medical knowledge governance and retrieval primitives
**Status: PARTIAL IMPLEMENTATION — no approved medical corpus, no deployed E5 ONNX model.**

- Reproducible Python JSON->SQLite/FTS5 corpus builder with mandatory approved source rights, reviewer identity, SHA-256 hash, English/Telugu scope, source and chunk metadata; intentionally refuses unreviewed inputs.
- Kotlin pure in-memory retrieval engine with Unicode lexical matching and exact dense cosine plus RRF, top-K bound, reviewed/approved source filtering, evidence IDs, tests and bounded prompt helper.
- Tested with **synthetic fixtures only**; NOT a licensed or clinician-reviewed medical knowledge pack. Do not use synthetic test fixtures as real medical information.
- Next: license-audited material, real clinical reviewer, Telugu translation validation, E5-small ONNX conversion, tokenizer golden parity, SQLite Android integration, bilingual Recall@K tests.
- No inference or clinical quality claimed.

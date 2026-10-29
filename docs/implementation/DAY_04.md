# Day 04 — Embedding model, semantic search and offline RAG

**Date:** 2026-10-29  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Implement the [40-section README](../../README.md) gradually, not a fake demo.  
**Outcome:** Find the right medical chunk locally using real multilingual embeddings, then connect retrieval to typed Android questions.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. The checkboxes are your personal tracker; leave unchecked until a real result exists. Learning hours and tests may extend past the calendar day; never mark unverified work complete.

## Task A — Learn before coding

- [ ] **01.** Draw numerical vectors and calculate dot product, vector norm and cosine similarity manually.
- [ ] **02.** Study tokenizers, pooling, attention masks and the exact inference recipe of the embedding model.
- [ ] **03.** Study multilingual E5 query/passage prefixes and how to evaluate Telugu/Tamil retrieval rather than assume it works.
- [ ] **04.** Read ONNX Runtime Mobile or LiteRT Android model execution and tokenizer integration constraints.
- [ ] **05.** Learn Recall@K, MRR, relevance thresholds and false retrieval/fallback decisions.

## Task B — Setup and environment

- [ ] **01.** Install/pin Python sentence-embedding dependencies for the offline indexing script.
- [ ] **02.** Choose a small multilingual embedding model with license and Android-acceptable compute cost.
- [ ] **03.** Build all static document vectors OFF-device, save model version and vector dimension alongside chunks.
- [ ] **04.** Set up ONNX Runtime Android or a tested LiteRT-compatible model; verify supported operators and RAM needs.
- [ ] **05.** Decide initial exact cosine retrieval and local Room/SQLite vector metadata; do not introduce a cloud vector DB.

## Task C — Small isolated exercise

- [ ] **01.** Embed ten nonmedical sample sentences and rank them against two search queries.
- [ ] **02.** Manually calculate cosine similarity of two small 3D vectors and compare to code.
- [ ] **03.** Measure how relevant candidates change when queries are rewritten into Telugu/Tamil.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Create normalized query embedding adapter behind EmbeddingEngine interface.
- [ ] **02.** Write a repeatable corpus vectorization/export script; use correct pooling and E5 formatting when applicable.
- [ ] **03.** Create a local index including vector_id, chunk_id, float vector, corpus hash and embedding-model version.
- [ ] **04.** Implement exact cosine top-K search with bounds, metadata filtering and minimum relevance threshold.
- [ ] **05.** Load precomputed vectors and chunk metadata on the Android device from an app-private package.
- [ ] **06.** Implement a Retriever returning chunk text plus complete source citations.
- [ ] **07.** Expose source title, date, language and link/reference in UI; handle missing provenance as a fault.
- [ ] **08.** Implement query-language routing and a deliberate no-relevant-source result.
- [ ] **09.** Build a prompt-context selector enforcing max chunks and max bytes/tokens.
- [ ] **10.** Create tests for corrupted index, dimension mismatch, zero vectors, empty corpus and index/model version mismatch.
- [ ] **11.** Compare Python reference retrieval with Android retrieval on the same golden queries.

## Task E — Experiments / learn by comparison

- [ ] **01.** Benchmark exact search speed for a few hundred, thousands and larger simulated vectors as memory allows.
- [ ] **02.** Measure Recall@1/3/5 separately for English/Telugu/Tamil on labelled questions.
- [ ] **03.** Test unrelated and ambiguous questions and assess whether threshold correctly declines retrieval.

## Task F — Testing and verification

- [ ] **01.** Runtime search is 100% local after index installation.
- [ ] **02.** Selected chunks belong to the true corpus, not LLM inventions.
- [ ] **03.** Each retrieved result carries its source metadata.
- [ ] **04.** Android and Python golden top-K outputs agree within numerical tolerance.
- [ ] **05.** Low-confidence or unavailable retrieval returns an explicit non-answer state.

## Task G — Proof of completion

- [ ] **01.** Working offline index builder and on-device query embedding/retrieval module.
- [ ] **02.** Unit tests, golden retrieval labels, per-language metric table and screenshots.
- [ ] **03.** Corpus/index/model manifests and reproducible benchmark report.
- [ ] Record exact versions, device/OS specs, commands run and logs in `docs/learning/day-04-notes.md`.
- [ ] Write three sentences in your own words: what you built, how it works, and what remains broken.
- [ ] Run `git status`, review your diff, run relevant tests, then commit with `day 04: <verified result>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) accurately.

## Exit criteria (mandatory)

A typed question must return relevant, sourced local chunks offline; generation is NOT required today.

## Official learning references

- [multilingual-e5-small model card](https://huggingface.co/intfloat/multilingual-e5-small)
- [ONNX Runtime Android](https://onnxruntime.ai/docs/get-started/with-android.html)
- [Room persistence](https://developer.android.com/training/data-storage/room)

## When blocked

1. Preserve the failing command, console output, device/runtime details and the expected result.
2. Ask AI to explain the error and give **one hint**; try your own fix first.
3. Write a minimal reproduction, apply a verified fix, rerun tests and document the result.
4. If model/hardware/data access is impossible, mark **BLOCKED** with evidence and use a clearly labelled interface-level fallback for development; do **not** claim the real feature works.

---

**Next:** [Day 05](DAY_05.md).

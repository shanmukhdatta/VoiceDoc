# Day 03 — Medical knowledge ingestion with traceable sources

**Date:** 2026-10-28  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Implement the [40-section README](../../README.md) gradually, not a fake demo.  
**Outcome:** Create an offline, source-licensed, reviewable medical-information corpus; build the data pipeline separately before Android integration.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. The checkboxes are your personal tracker; leave unchecked until a real result exists. Learning hours and tests may extend past the calendar day; never mark unverified work complete.

## Task A — Learn before coding

- [ ] **01.** Learn how RAG differs from model training and why untrusted user chats must not join the knowledge base.
- [ ] **02.** Study data provenance, licensing, publication dates, source versioning and source metadata.
- [ ] **03.** Read Python virtual environments, pathlib, JSONL, logging, pytest and type hints.
- [ ] **04.** Study HTML/PDF parsing, cleaning, deduplication, section-aware chunks and overlapping windows.
- [ ] **05.** Understand train/dev/test separation, multilingual quality control and the danger of assuming translation is clinically accurate.

## Task B — Setup and environment

- [ ] **01.** Create `rag/ingestion`, `rag/cleaning`, `rag/chunking`, `rag/schemas`, `knowledge/source_manifests` and `knowledge/licenses` only as needed.
- [ ] **02.** Create and pin a minimal Python environment; add requirements/lockfile and reproducible CLI usage.
- [ ] **03.** Choose a small, clearly licensed starter collection of trusted public-health information; record URLs, terms and permitted reuse.
- [ ] **04.** Define a schema with source_id, title, publisher, URL, version/date, license, language, section, chunk_id, hash and review status.

## Task C — Small isolated exercise

- [ ] **01.** Take one licensed informational document and parse its headings and paragraphs.
- [ ] **02.** Write a chunker that preserves section title, nearby context and references.
- [ ] **03.** Write tests for duplicate text, blank sections, malformed encodings and Unicode.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Create ingestion scripts accepting reproducible document manifests instead of random internet scraping.
- [ ] **02.** Preserve original source information and transformed text; never silently erase attribution.
- [ ] **03.** Normalize encoding, whitespace and headings while preserving numerals, dosages and negations.
- [ ] **04.** Deduplicate by normalized hashes; log removed duplicates and reasons.
- [ ] **05.** Apply section-aware chunking with tunable chunk size and overlap; prevent truncated facts without context.
- [ ] **06.** Store corpus as JSONL with stable IDs and an explicit schema version.
- [ ] **07.** Create a source registry including publication/update date, review/expiry and allowed-use notes.
- [ ] **08.** Add English, Telugu and Tamil evaluation questions with verified relevant source IDs; label translated items needing review.
- [ ] **09.** Build offline corpus packaging (`rag/build_knowledge_pack.py` or equivalent) with hash verification.
- [ ] **10.** Create a small knowledge QA CLI to lookup source/chunk by ID.
- [ ] **11.** Keep medical content separate from user conversations and logging.
- [ ] **12.** Write tests that fail if any chunk lacks publisher/source/license or if a prohibited source is included.

## Task E — Experiments / learn by comparison

- [ ] **01.** Compare two chunk sizes and report chunks per source plus lost-context examples.
- [ ] **02.** Inspect at least 20 randomly sampled chunks manually for provenance and meaning.
- [ ] **03.** Try malformed document input and confirm helpful failure instead of half-written index.

## Task F — Testing and verification

- [ ] **01.** Every released chunk maps back to an identifiable source and license.
- [ ] **02.** No patient data or unsanctioned/private documents enter corpus.
- [ ] **03.** Deterministic build produces stable content hashes.
- [ ] **04.** Language tags and review statuses are explicit, not guessed.
- [ ] **05.** Corpus can be bundled locally on Android without a server dependency.

## Task G — Proof of completion

- [ ] **01.** `knowledge/source_manifests/` registry and corresponding licenses/usage notes.
- [ ] **02.** Python ingestion/cleaning/chunking/export scripts, schema, tests and dataset card.
- [ ] **03.** Offline chunk package, hash manifest and small retrieval evaluation set.
- [ ] Record exact versions, device/OS specs, commands run and logs in `docs/learning/day-03-notes.md`.
- [ ] Write three sentences in your own words: what you built, how it works, and what remains broken.
- [ ] Run `git status`, review your diff, run relevant tests, then commit with `day 03: <verified result>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) accurately.

## Exit criteria (mandatory)

Do not proceed until sources are traceable and license/review gaps are visibly documented; a medical corpus with unverifiable sources is not acceptable.

## Official learning references

- [MedlinePlus API & linking resources](https://medlineplus.gov/about/developers/)
- [Hugging Face datasets documentation](https://huggingface.co/docs/datasets/index)
- [Python packaging and venv](https://docs.python.org/3/tutorial/venv.html)

## When blocked

1. Preserve the failing command, console output, device/runtime details and the expected result.
2. Ask AI to explain the error and give **one hint**; try your own fix first.
3. Write a minimal reproduction, apply a verified fix, rerun tests and document the result.
4. If model/hardware/data access is impossible, mark **BLOCKED** with evidence and use a clearly labelled interface-level fallback for development; do **not** claim the real feature works.

---

**Next:** [Day 04](DAY_04.md).

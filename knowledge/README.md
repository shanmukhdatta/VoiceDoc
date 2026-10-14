# Local medical knowledge governance
No real medical corpus is bundled. JSON sources for `rag/ingestion/build_pack.py` must be explicitly rights-approved, clinician-reviewed, language-tagged, publication/provenance-checked and content-hashed. Do not assume a public medical website authorizes redistribution or automated translation. Test fixtures must never be offered as health guidance.

The SQL builder stores source and chunk provenance and builds FTS5 locally. Real multilingual E5 vectors, validated medical passages and retrieval thresholds are future gated work.

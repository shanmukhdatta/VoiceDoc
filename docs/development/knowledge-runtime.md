# Runtime on-device SQLite knowledge viewer

The Android Sources screen can import a developer-built SQLite pack using the standard file picker (no INTERNET permission), check basic schema/provenance fields, compute a local SHA-256 digest, list the source metadata and search passages locally with a parameterized query. File remains in private no-backup storage, can be deleted in UI.

Pack builder: `python rag/ingestion/build_pack.py --input approved.json --output approved.sqlite`. The build script only checks a self-asserted `rights_verified` and `clinician_reviewed` field; **it does not independently confirm the provenance or obtain a medical professional's approval**. Genuine source packs need external legal and clinical review before acceptance. A local hash protects bytes against accidental modification, **not against a forged upstream file**.

Search uses built-in Android SQLite and a conservative LIKE fallback; no claims of deployed E5 multilingual embedding or measurement-validated dense retrieval. The standalone Kotlin exact cosine + RRF code is present in core; runtime E5 ONNX/model conversion and tokenizer parity still need implementation.

# Voice Doctor AI local model artifacts

No model binaries are committed here. Every model pack needs its canonical source/revision, license text and redistribution review, original and converted SHA-256, exact byte counts, format, tokenizer, language capability, Android runtime/version, and a measured phone load.

Allowed initial languages: `en` and `te` only. Gemma 3 weights require Gemma terms; renaming a file to `.litertlm` does not make it a valid LiteRT-LM artifact. Whisper GGML, E5 ONNX and Telugu TTS rights must be reviewed individually.

## Android generation-pack importer

The Models screen currently accepts schema-v1 **generation** ZIP packs for LiteRT-LM Android 0.18.0. A pack contains `manifest.json`, one `litertlm` model, and a tokenizer asset; the ZIP entries must exactly match those declared paths. The manifest schema and file checks are documented in [`ml/preparation/README.md`](../ml/preparation/README.md).

Before import, obtain the SHA-256 of the exact `manifest.json` from an independently trusted publisher channel. Enter that digest in the app. The importer verifies the supplied digest, declared compatibility, paths, sizes, ZIP contents, and asset hashes, then installs atomically in app-private no-backup storage. It rejects repeat installs of the same pack ID/version; remove the prior pack before replacing it. The current digest field is a developer trust input: the app does not verify publisher signatures or fetch trusted metadata itself.

Integrity and compatibility checks do not establish publisher authenticity, redistribution rights, tokenizer equivalence, model quality, or clinical safety. Imported generation packs are **not connected to medical chat**. Do not treat a successful import as model-load or clinical validation.

Gemma 3 1B weights are subject to Gemma terms. No model files are committed or included in the APK.

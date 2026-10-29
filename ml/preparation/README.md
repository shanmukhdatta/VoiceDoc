# Model training, export and integrity prerequisites

This directory contains actual reproducible preparation programs, NOT trained model artifacts.

### Source approval + grouping
`python ml/preparation/prepare_reviewed_sft.py --input fully-reviewed.jsonl --output-dir data/splits --seed 42`

Every input row requires source ID, license, approved rights, clinician review identity, language `en`/`te` and scenario ID. Group-based split prevents scenario overlap. Legal and reviewer claims still require **independent external verification**; flags cannot prove credentials.

### Embedding model export
Install model-specific Python packages in a disposable isolated environment (torch, transformers, onnx), and obtain the exact **MIT-licensed** `intfloat/multilingual-e5-small` model/tokenizer revision through an approved model acquisition process. Do not redistribute artifacts without checking their terms.

`python ml/preparation/export_e5_onnx.py --local-model /path/to/approved-local-e5 --output /path/to/empty-output`

This exports `encoder.onnx`, tokenizer files, and gold reference token IDs/masks/vector values, which must match Android ONNX execution. **ONNX graph export has not been executed in ChatGPT CI**, because weights/heavy libraries are absent. Android E5 free-text tokenization still requires a matching SentencePiece/Tokenizer backend, so don't call dense Telugu retrieval production-ready.

### Versioned artifact packs
`model_manifest.py` accepts a schema-v1 pack object with pack ID/version/kind, exact runtime name/version, English/Telugu declarations, optional embedding dimension, and per-file role/path/format/size/SHA/source/revision/license metadata. Generation packs require a LiteRT-LM model and tokenizer; embedding packs require an ONNX model and tokenizer; speech packs require a model; knowledge packs require a SQLite index. Paths must stay under the pack root, all declared file bytes are checked, and duplicate roles/IDs/paths are rejected.

```powershell
python ml/preparation/model_manifest.py --manifest local-pack.json --root model-assets/ --expected-runtime onnxruntime-android --expected-runtime-version 1.30.0 --expected-embedding-dimension 384
```

Use expected runtime/dimension flags matching the target app. This offline preflight verifies declared compatibility and bytes only. A self-authored JSON file cannot prove publisher authenticity, redistribution permission, clinical suitability, or that Android can load an artifact. The Android Models screen now imports generation packs for LiteRT-LM Android 0.18.0 when supplied an independently trusted manifest SHA-256. The importer verifies archive contents and file integrity but does not verify signatures or connect imported packs to medical chat. See [`models/README.md`](../../models/README.md).

### Device inference and fine-tuning
Gemma 3 1B IT quantization and LiteRT-LM packaging must follow the official supported toolchain for the chosen exact revision; converting arbitrary LoRA adapters directly into `.litertlm` is not guaranteed. Heavy Kaggle training starts only with reviewed and legally usable corpora and a defined baseline comparison. No training has been performed, no accuracy is asserted.

References:
- https://huggingface.co/intfloat/multilingual-e5-small
- https://developers.google.com/edge/litert-lm/android
- https://github.com/ggml-org/whisper.cpp

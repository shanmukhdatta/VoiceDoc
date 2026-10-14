# Model training, export and integrity prerequisites

This directory contains actual reproducible preparation programs, NOT trained model artifacts.

### Source approval + grouping
`python ml/preparation/prepare_reviewed_sft.py --input fully-reviewed.jsonl --output-dir data/splits --seed 42`

Every input row requires source ID, license, approved rights, clinician review identity, language `en`/`te` and scenario ID. Group-based split prevents scenario overlap. Legal and reviewer claims still require **independent external verification**; flags cannot prove credentials.

### Embedding model export
Install model-specific Python packages in a disposable isolated environment (torch, transformers, onnx), and obtain the exact **MIT-licensed** `intfloat/multilingual-e5-small` model/tokenizer revision through an approved model acquisition process. Do not redistribute artifacts without checking their terms.

`python ml/preparation/export_e5_onnx.py --local-model /path/to/approved-local-e5 --output /path/to/empty-output`

This exports `encoder.onnx`, tokenizer files, and gold reference token IDs/masks/vector values, which must match Android ONNX execution. **ONNX graph export has not been executed in ChatGPT CI**, because weights/heavy libraries are absent. Android E5 free-text tokenization still requires a matching SentencePiece/Tokenizer backend, so don't call dense Telugu retrieval production-ready.

### Artifact verification
`python ml/preparation/model_manifest.py --manifest local-manifest.json --root model-assets/`

Requires expected publisher hash, size, exact pinned revision, language tags, format and independently authorized redistribution. Integrity and right-to-use checks remain separate; user-provided JSON approval booleans do not make a model legal.

### Device inference and fine-tuning
Gemma 3 1B IT quantization and LiteRT-LM packaging must follow the official supported toolchain for the chosen exact revision; converting arbitrary LoRA adapters directly into `.litertlm` is not guaranteed. Heavy Kaggle training starts only with reviewed and legally usable corpora and a defined baseline comparison. No training has been performed, no accuracy is asserted.

References:
- https://huggingface.co/intfloat/multilingual-e5-small
- https://developers.google.com/edge/litert-lm/android
- https://github.com/ggml-org/whisper.cpp

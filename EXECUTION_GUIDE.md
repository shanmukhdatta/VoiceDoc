# VoiceDoc Execution Guide

This guide provides the exact end-to-end sequence to fine-tune Qwen3-0.6B, convert and quantize it for LiteRT-LM Android (`0.18.0`), export `multilingual-E5-small` to ONNX, build the medical SQLite + vector knowledge base, package `VoiceDoc_Model_Pack.zip`, build the Android APK, and verify offline English & Telugu functionality on a physical ARM64 Android phone.

---

## Environment Split

| Stage | Where It Runs |
| :--- | :--- |
| 1. Clone / update `main` & review dataset | Local MacBook |
| 2. QLoRA fine-tuning & adapter merge | Kaggle GPU (`CUDA_VISIBLE_DEVICES=0`) |
| 3. Model conversion (`.litertlm`), E5 ONNX export, RAG DB + embeddings, `VoiceDoc_Model_Pack.zip`, Android APK build | Local MacBook |
| 4. APK install, model pack import, offline English & Telugu verification | Physical ARM64 Android device (Airplane Mode) |

---

## Step 1 — Clone or Update `main` (Local MacBook)

```bash
git clone https://github.com/shanmukhdatta/VoiceDoc.git
cd VoiceDoc
git checkout main && git pull origin main
```

---

## Step 2 — Review and Prepare Approved Dataset Exports (Local MacBook)

> [!IMPORTANT]
> Candidate records in `dataset/sft/candidates/` and `dataset/rag/candidates/` require explicit human/clinician review (`rights_verified: true`, `clinician_reviewed: true`, `reviewed_by`, and cryptographic review receipts) before training or RAG ingestion.

Validate dataset integrity and prepare reviewed SFT splits:

```bash
python3 dataset/scripts/validate_dataset.py
python3 ml/preparation/prepare_reviewed_sft.py \
  --input /path/to/approved-reviewed-sft.jsonl \
  --output-dir ./work/sft-splits \
  --seed 42
```

---

## Step 3 — Open the Kaggle Training Notebook (Kaggle GPU)

1. Upload or attach the `VoiceDoc` repository, your approved `reviewed-sft.jsonl`, and a local snapshot of `Qwen/Qwen3-0.6B` (with its exact 40-character Hugging Face commit SHA) as Kaggle Inputs.
2. Open `ml/training/train_kaggle.ipynb` in a **single-GPU** Kaggle session.
3. Update the path variables in Cell 1 (`REPO`, `REVIEWED`, `MODEL`, `REVISION`, `RUN`).

---

## Step 4 — Execute QLoRA Fine-Tuning (Kaggle GPU)

Run all cells in `ml/training/train_kaggle.ipynb` sequentially, or execute directly:

```bash
python -m pip install -r ml/training/requirements-kaggle.txt
python ml/training/run_kaggle.py \
  --reviewed-sft /kaggle/input/voicedoc-reviewed/reviewed-sft.jsonl \
  --base-model-dir /kaggle/input/qwen3-06b/snapshot \
  --base-model-revision <EXACT_40_CHAR_HF_COMMIT_SHA> \
  --run-dir /kaggle/working/voicedoc
```

- Uses NF4 4-bit QLoRA, Qwen3 non-thinking chat template (`enable_thinking=False`), assistant-only loss masking, gradient checkpointing, and checkpoint recovery (`--resume-from-checkpoint`).

---

## Step 5 — Save and Retrieve the Merged Model (Kaggle → Local MacBook)

`run_kaggle.py` automatically invokes `ml/training/merge_adapter.py` to merge the best LoRA adapter into CPU FP32 base weights and export FP16 Safetensors to `/kaggle/working/voicedoc/merged/`.

Download `/kaggle/working/voicedoc/merged/` and `/kaggle/working/voicedoc/training/run-metadata.json` to your local MacBook (e.g., `./artifacts/qwen3-merged/`).

---

## Step 6 — Convert and Quantize the Model (Local MacBook)

Install `litert-torch` in an isolated Python 3.11/3.12 virtual environment:

```bash
python3 -m venv .venv-convert && source .venv-convert/bin/activate
pip install --upgrade pip
pip install litert-torch ai-edge-litert ai-edge-quantizer torch transformers
```

Validate and convert the merged Qwen3-0.6B Safetensors checkpoint to `.litertlm` (INT4 `dynamic_wi4b32_afp32` or INT8 `dynamic_wi8_afp32`):

```bash
python ml/preparation/convert_qwen3_litertlm.py \
  --merged-dir ./artifacts/qwen3-merged \
  --output-dir ./artifacts/qwen3-litertlm \
  --backend litertlm \
  --quantization int4 \
  --context-length 2048
```

*(Documented fallback if LiteRT-LM conversion is unavailable on host: `--backend gguf --llama-cpp-dir /path/to/llama.cpp`.)*

---

## Step 7 — Export the Multilingual Embedding Model (Local MacBook)

Obtain a local snapshot of `intfloat/multilingual-e5-small` (MIT-licensed) and export `encoder.onnx` + `tokenizer/tokenizer.json` + `golden-reference.json`:

```bash
pip install onnx onnxruntime numpy
python ml/preparation/export_e5_onnx.py \
  --local-model /path/to/multilingual-e5-small \
  --output ./artifacts/e5-onnx
```

---

## Step 8 & 9 — Build the Approved Medical Knowledge Database and Generate Document Embeddings (Local MacBook)

1. Export chunk JSONL from your clinician-reviewed RAG sources and compute 384-dim L2-normalized embeddings with `"passage: "` prefix:

```bash
python ml/preparation/embed_corpus.py \
  --onnx ./artifacts/e5-onnx/encoder.onnx \
  --tokenizer-dir ./artifacts/e5-onnx/tokenizer \
  --input-jsonl ./work/approved-rag-chunks.jsonl \
  --output-json ./work/chunk-embeddings.json \
  --prefix "passage: "
```

2. Build the SQLite knowledge database (FTS5 + `chunk_embeddings` table):

```bash
python rag/ingestion/build_pack.py \
  --input /path/to/approved-rag-sources.json \
  --output ./artifacts/voicedoc-knowledge.sqlite \
  --embeddings-json ./work/chunk-embeddings.json
```

---

## Step 10 — Create the Complete `VoiceDoc_Model_Pack.zip` (Local MacBook)

```bash
python ml/preparation/create_voicedoc_pack.py \
  --llm-model ./artifacts/qwen3-litertlm/<model>.litertlm \
  --e5-onnx ./artifacts/e5-onnx/encoder.onnx \
  --e5-tokenizer ./artifacts/e5-onnx/tokenizer/tokenizer.json \
  --knowledge-db ./artifacts/voicedoc-knowledge.sqlite \
  --output ./artifacts/VoiceDoc_Model_Pack.zip
```

This prints the trusted `manifest_sha256` and writes `./artifacts/VoiceDoc_Model_Pack.manifest.sha256`.

---

## Step 11 — Build the Android APK (Local MacBook)

Requires JDK 17+ and Android SDK (compileSdk 36, minSdk 29):

```bash
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
```

Verify 16 KiB native library page-size alignment:

```bash
python3 scripts/audit_native_16k.py android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 12 — Install the APK on a Physical ARM64 Phone

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb push ./artifacts/VoiceDoc_Model_Pack.zip /sdcard/Download/
```

---

## Step 13 — Import the Model Pack in VoiceDoc

1. Open **Voice Doctor AI** on the phone and complete onboarding.
2. Go to the **Models** tab.
3. Paste the 64-character `manifest_sha256` from `./artifacts/VoiceDoc_Model_Pack.manifest.sha256`.
4. Tap **Import VoiceDoc_Model_Pack.zip (Full Bundle)** and select `/sdcard/Download/VoiceDoc_Model_Pack.zip`.

---

## Step 14 — Verify Offline English and Telugu Functionality

1. Enable **Airplane Mode** on the phone.
2. Open the **Chat** tab:
   - Verify the status banner shows **Offline AI status: Offline Qwen3 + E5 semantic + FTS5 retrieval ready**.
   - Test an English query and a Telugu query (`Settings → Language → తెలుగు`), verifying grounded answers with `[source_id]` citations.
   - Test an urgent query (e.g., `"chest pain"`) and verify immediate safety escalation without waiting for the LLM.
   - Test **Speak to your assistant** (if an Android on-device speech pack is installed; otherwise verify clear text-mode fallback) and **Speak latest system notice offline** (offline TTS).

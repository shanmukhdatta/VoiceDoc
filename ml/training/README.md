# VoiceDoc single-run Qwen3-0.6B QLoRA

Extends the existing training code. No model downloads, training, tokenizer execution or tests performed during authoring. Current inherited data has 60 SFT candidates and zero clinical approvals. Obtain the completed data and genuine reviewer receipts before training; see dataset/TRAINING_HANDOFF.md. No bypass of the existing approval gate.

## Antigravity / Kaggle

Attach the repository, approved review file and complete local Qwen/Qwen3-0.6B safetensors snapshot to Kaggle. Select one CUDA GPU. Edit paths and exact publisher commit SHA in train_kaggle.ipynb. Install requirements-kaggle.txt retaining the compatible Kaggle CUDA PyTorch build. Run the notebook or execute:

```sh
CUDA_VISIBLE_DEVICES=0 python ml/training/run_kaggle.py \
  --reviewed-sft /path/to/reviewed-sft.jsonl \
  --base-model-dir /path/to/pinned-qwen3 \
  --base-model-revision ACTUAL_40_CHAR_HF_SHA \
  --run-dir /kaggle/working/voicedoc
```

Use --prepare-only to perform reviewed export and real tokenizer checks without GPU training. Resume with --resume-from-checkpoint /kaggle/working/voicedoc/training/checkpoints/checkpoint-N and identical input/model/code/config. Persist the whole run directory to recover after Kaggle interruption. If merge alone fails, rerun merge_adapter.py with --base-model-dir and --run-dir.

Outputs: approved split files, input-manifest.json, checkpoints, training/adapter, training/run-metadata.json (validation and held-out loss), merged/ FP16 checkpoint. Save these as Kaggle output artifacts. The merged checkpoint is not yet an Android runtime package.

## Design choices

NF4 QLoRA with double quantization, rank 16, alpha 32, attention and FFN projections, batch 1 and accumulation 8, max 2048, 3 epochs, seed 42. Configurable defaults, not tuned. Uses native Qwen3 non-thinking template and assistant-only labels, rejects overlength examples and mismatched prompt prefixes. SFT system prompt now allows cautious general information without supplied evidence and requires grounding when provided; production RAG policy remains unchanged. Validation loss selects best adapter, held-out test loss is reported once; neither establishes clinical quality. Reload original unquantized base in CPU FP32 for merge and export FP16. Verify actual compatibility and memory capacity when running. No Android modifications or experiment suite.

Run tests later: python -m unittest discover -s ml/training -p 'test_*.py'.

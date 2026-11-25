# VoiceDoc dataset and Kaggle handoff

Reference revision: `9983139cf469f1ab30cbb29d80841aa9fabc5a46`.

| Input | Location | Count | Status |
|---|---|---:|---|
| SFT prompt/response JSONL | `dataset/sft/candidates/sft_complete.jsonl` | 80 | Review candidate |
| SFT candidate splits | `dataset/sft/splits/{train,validation,test}.jsonl` | 64 / 8 / 8 | Review candidate |
| RAG chunk JSONL | `dataset/rag/candidates/rag_complete.jsonl` | 40 | Review candidate |
| RAG native-shaped source JSON | `dataset/rag/source_export/candidate_sources.json` | 40 | Review candidate |
| Clinical review queue | `dataset/reports/medical_review_queue.jsonl` | 120 | Pending |
| Training and pack approvals | `dataset/sft/approved/`, `dataset/rag/approved/` | 0 | Awaiting documented approvals |

The [schema](DATASET_SPECIFICATION.md) documents all fields. For continuing authoring, `dataset/sources/authoring/sft.tsv` has topic slug, scenario slug, capability, style, risk, utterance, final answer, and optional conversation history separated by pipes. History has three utterances separated by tildes. `rag.tsv` has topic slug, English text and Telugu text. Keep literal pipes and line breaks out of TSV cells. To add another bilingual RAG passage for the same source, use a stable `topic@facet` key in the RAG TSV. A grounded SFT example can use that same key to select its evidence. All examples from that source topic stay in one split group.

## Review and exports

Review each candidate against its cited Summary. Seek qualified clinical review and Telugu-language assessment. The review queue includes a record digest. For an unchanged, genuinely approved record, copy the exact candidate row to a separate reviewed JSONL, set `clinician_reviewed` to `true`, set `reviewed_by` to the real reviewer's identifier, and add an `approval` object containing `reviewer` (same identifier), `review_reference` (a genuine traceable reference), and `record_sha256` (queue digest). Regenerate edited candidates and their review digests before approval. The utility verifies the binding to the candidate; the reviewer identity and evidence must be assessed in the review workflow.

```sh
python3 dataset/scripts/prepare_training_export.py \
  --reviewed /path/to/reviewed-sft.jsonl --output-dir dataset/sft/approved
python3 dataset/scripts/prepare_rag_export.py \
  --reviewed /path/to/reviewed-rag.jsonl --output dataset/rag/approved/sources.json
python3 rag/ingestion/build_pack.py \
  --input dataset/rag/approved/sources.json --output /path/to/new-knowledge.sqlite
```

The SFT export checks the original VoiceDoc preparation and training gates, keeps the seeded three-way assignments, and requires all three approved splits to contain records. The original `ml/preparation/prepare_reviewed_sft.py` performs its own two-way split. Use the dataset export above to retain the held-out test file. The RAG builder requires a fresh SQLite output path and internally rechunks source text into 800-character windows.

## Load approved JSONL on Kaggle

Upload the repository and the locally verified snapshot of `Qwen/Qwen3-0.6B` as notebook inputs. Record the exact Hugging Face revision. Copy any writable output to `/kaggle/working`. Install the repository's `ml/training/requirements-kaggle.txt`. For JSONL inspection and analysis, load all approved splits via Hugging Face Datasets.

```python
from datasets import load_dataset

names = ("train", "validation", "test")
rows = load_dataset(
    "json",
    data_files={split: f"dataset/sft/approved/{split}.jsonl" for split in names},
)
```

See the [Hugging Face JSON loader documentation](https://huggingface.co/docs/datasets/loading#json).

The unmodified trainer renders its system and user prompt with the model-native Qwen3 chat template, renders the full sequence with the assistant response, checks the template prefix, and sets `-100` labels over the prompt tokens. Supply the raw `prompt` and `response` fields directly. The repository trainer uses the tokenizer's default thinking setting. Qwen3 supports explicitly disabling thinking in deployments that expose that setting. The authored answers contain no reasoning tags. See the [Qwen3 model card](https://huggingface.co/Qwen/Qwen3-0.6B).

Run the tokenizer audit against the pinned assets before training. This uses the exact tokenization function in VoiceDoc, requires no model weights in memory, and stops if a sequence exceeds the specified limit.

```sh
python3 dataset/scripts/validate_dataset.py --model-dir /path/to/pinned-qwen-snapshot --max-seq-len 2048
```

A provisional Kaggle starting configuration is one available GPU, batch size 1, gradient accumulation 8, sequence cap 2,048, LoRA rank 16, alpha 32, dropout 0.05 and learning rate 2e-4. Consider these repository defaults for an approved, sufficiently sized dataset. See the trainer's device and precision selection before choosing hardware. Reserve the test set for final evaluation and use the validation set for iterative decisions.

The command below verifies the approved files via the trainer without loading models or using a GPU. Supply actual local paths and the pinned snapshot revision.

```sh
python3 ml/training/train_qlora.py \
  --train-jsonl dataset/sft/approved/train.jsonl \
  --validation-jsonl dataset/sft/approved/validation.jsonl \
  --base-model-dir /path/to/pinned-qwen-snapshot \
  --base-model-revision ACTUAL_HUGGING_FACE_REVISION \
  --output-dir /kaggle/working/voicedoc-qlora --validate-only
```

The candidate release has 9,920 SFT records and 2,960 RAG chunks remaining against the targets. Recover full desired topic, capability and language coverage, complete clinical and linguistic review, audit the pinned tokenizer and validate the approved export before any training or release use.

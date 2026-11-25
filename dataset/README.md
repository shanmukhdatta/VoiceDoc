# VoiceDoc dataset preparation

This partial delivery contains **80 SFT candidates and 1,520 RAG candidates** across all 15 requested domains. RAG includes 1,500 English and 20 Telugu chunks. Clinical approvals total zero. Use the candidates for review and dataset engineering. The unchanged repository gates require clinician approval before training or building a real knowledge pack.

| Contents | Path |
|---|---|
| SFT candidates | [sft_complete.jsonl](sft/candidates/sft_complete.jsonl) |
| Topic-grouped candidate splits | [sft/splits](sft/splits) |
| RAG candidates | [rag_complete.jsonl](rag/candidates/rag_complete.jsonl) |
| Source-level candidate preview | [candidate_sources.json](rag/source_export/candidate_sources.json) |
| Registry and rights evidence | [sources](sources) |
| Candidate review queue | [medical_review_queue.jsonl](reports/medical_review_queue.jsonl) |
| Verification and remaining work | [final_quality_report.md](reports/final_quality_report.md) |
| Kaggle handoff | [TRAINING_HANDOFF.md](TRAINING_HANDOFF.md) |

From the repository root, run the following with Python 3.9 or newer to regenerate the committed candidates from the human-readable authoring inputs.

```sh
export PYTHONDONTWRITEBYTECODE=1
python3 dataset/scripts/generate_dataset.py
python3 dataset/scripts/split_dataset.py --seed 42
python3 dataset/scripts/deduplicate.py
python3 dataset/scripts/validate_dataset.py
python3 dataset/scripts/prepare_rag_export.py --candidate --output dataset/rag/source_export/candidate_sources.json
python3 dataset/scripts/compute_statistics.py
python3 -m unittest discover -s dataset/tests
```

Approved directories are created by the export utilities once reviewed inputs are provided. Keep reviewer receipts under access appropriate to the reviewer. Source: MedlinePlus, National Library of Medicine.

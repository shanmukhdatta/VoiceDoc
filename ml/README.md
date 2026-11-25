# Offline model evaluation (initial implementation)

Python 3.11 standard library only. Run:

```bash
python -m unittest discover -s ml/evaluation -p "test_*.py" -v
python ml/evaluation/metrics.py --input private-evaluation.jsonl --output report.json
```

Input JSONL: each record has `language` = `en`, `te` or `en-te`, `task` = `asr`, `retrieval` or `urgency`. ASR records have `reference` and `prediction`. Retrieval records have `relevant_ids` and `retrieved_ids` (arrays). Urgency records have boolean `urgent_gold` and `escalated`.

Output includes WER, CER, Recall@3/5 and urgent-case escalation recall per language. Synthetic unit tests do **not** show medical quality. Required later: professional gold annotations, held-out test sets, medical entity/number/negation ASR accuracy, clinical false-reassurance evaluation, robust uncertainty/grounding benchmarks and real-phone latency/RAM tests. Do not check private medical datasets into Git.

Training/ONNX conversion, Gemma quantization, Whisper native Android runtime, LiteRT-LM adapter and Telugu licensed voice export are not yet implemented or verified.

"""Pure Python offline metrics. Results are NOT clinical validation."""
from __future__ import annotations
import argparse
import json
from collections import defaultdict
from pathlib import Path

def edit_distance(a, b):
    prev = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        row = [i]
        for j, y in enumerate(b, 1):
            row.append(min(row[-1]+1, prev[j]+1, prev[j-1]+(x!=y)))
        prev = row
    return prev[-1]

def word_error_rate(pairs):
    words = edits = 0
    for reference, predicted in pairs:
        gold = reference.split()
        words += len(gold)
        edits += edit_distance(gold, predicted.split())
    return edits / words if words else None

def character_error_rate(pairs):
    chars = edits = 0
    for reference, predicted in pairs:
        chars += len(reference)
        edits += edit_distance(list(reference), list(predicted))
    return edits / chars if chars else None

def retrieval_recall(rows, k):
    if not rows:
        return None
    return sum(bool(set(r["relevant_ids"]) & set(r["retrieved_ids"][:k])) for r in rows) / len(rows)

def urgency_recall(rows):
    positives = [r for r in rows if r["urgent_gold"] is True]
    if not positives:
        return None
    return sum(r["escalated"] is True for r in positives) / len(positives)

def evaluate(rows):
    out = {}
    by_language = defaultdict(list)
    for r in rows:
        if r.get("language") not in {"en", "te", "en-te"}:
            raise ValueError("Unsupported evaluation language")
        if r.get("task") not in {"asr", "retrieval", "urgency"}:
            raise ValueError("Unsupported evaluation task")
        by_language[r["language"]].append(r)
    for language, records in sorted(by_language.items()):
        asr = [(r["reference"], r["prediction"]) for r in records if r["task"] == "asr"]
        retrieval = [r for r in records if r["task"] == "retrieval"]
        urgent = [r for r in records if r["task"] == "urgency"]
        if any(type(r.get("urgent_gold")) is not bool or type(r.get("escalated")) is not bool for r in urgent):
            raise ValueError("Urgency labels must be booleans")
        out[language] = {
            "asr_count": len(asr), "wer": word_error_rate(asr), "cer": character_error_rate(asr),
            "retrieval_count": len(retrieval),
            "recall_at_3": retrieval_recall(retrieval, 3),
            "recall_at_5": retrieval_recall(retrieval, 5),
            "urgency_count": len(urgent), "urgency_recall": urgency_recall(urgent)
        }
    return out

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--input", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    rows = [json.loads(s) for s in a.input.read_text(encoding="utf8").splitlines() if s.strip()]
    result = {"evaluated_cases": len(rows), "by_language": evaluate(rows)}
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(result, indent=2, ensure_ascii=False)+"\n", encoding="utf8")

if __name__ == "__main__":
    main()

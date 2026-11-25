"""Count actual candidates and summarize target shortfalls."""
from collections import Counter

from common import ROOT, SFT, RAG, CAPABILITIES, LANGUAGE_TARGETS, read_json, read_rows, write_json


def counts(rows, fields):
    return {field: dict(Counter(row[field] for row in rows)) for field in fields}


def main():
    sft, rag = read_rows(SFT), read_rows(RAG)
    sft_fields = ("language", "language_style", "domain", "capability", "risk_level")
    validation = read_json(ROOT / "reports/validation_results.json")
    duplicates = read_json(ROOT / "reports/duplicate_audit.json")
    results = {
        "sft": {"count": len(sft), "target": 10000, "remaining": max(0, 10000 - len(sft)),
                "distributions": counts(sft, sft_fields), "style_targets": LANGUAGE_TARGETS,
                "capability_targets": CAPABILITIES},
        "rag": {"count": len(rag), "target": 3000, "remaining": max(0, 3000 - len(rag)),
                "distributions": counts(rag, ("language", "domain")),
                "language_targets": {"en": 1500, "te": 1500}},
        "splits": {},
        "verified_registry_sources": sum(source["rights_verified"] is True for source in
                                         read_json(ROOT / "sources/source_registry.json")),
        "review_queue": len(read_rows(ROOT / "reports/medical_review_queue.jsonl")),
        "approved": {"sft": 0, "rag": 0},
        "validation_errors": validation["record_errors"],
        "deduplication": {key: {"exact_groups": len(value["exact_groups"]),
                               "near_pairs": len(value["near_pairs"])}
                          for key, value in duplicates.items() if isinstance(value, dict)},
    }
    for name in ("train", "validation", "test"):
        rows = read_rows(ROOT / f"sft/splits/{name}.jsonl")
        results["splits"][name] = {"count": len(rows), "distributions": counts(rows, sft_fields),
                                   "groups": sorted({row["split_group"] for row in rows})}
    write_json(ROOT / "reports/dataset_statistics.json", results)


if __name__ == "__main__":
    main()

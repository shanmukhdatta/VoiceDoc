"""Report exact and character-trigram similarity. Never delete rows automatically."""
import argparse
from collections import defaultdict
import unicodedata

from common import ROOT, SFT, RAG, read_rows, write_json


def normalized(text):
    folded = unicodedata.normalize("NFC", text).casefold()
    return " ".join("".join(
        char if unicodedata.category(char)[0] in "LMN" else " " for char in folded
    ).split())


def item_text(row, field):
    value = row[field]
    if field == "prompt" and row.get("evidence_chunk_ids"):
        value = value.split("\n\n", 1)[-1]
    return normalized(value)


def duplicates(rows, field, threshold=.78):
    index = defaultdict(list)
    exact = defaultdict(list)
    near = []
    ids = [row.get("id") or row["chunk_id"] for row in rows]
    for i, row in enumerate(rows):
        value = item_text(row, field)
        exact[value].append(ids[i])
        grams = {value[j:j + 3] for j in range(len(value) - 2)}
        overlap = defaultdict(int)
        for gram in grams:
            for prior, prior_length in index[gram]:
                overlap[prior, prior_length] += 1
            index[gram].append((i, len(grams)))
        for (prior, prior_length), shared in sorted(overlap.items()):
            union = len(grams) + prior_length - shared
            if shared / union >= threshold:
                other = item_text(rows[prior], field)
                if value != other:
                    near.append({"ids": [ids[prior], ids[i]], "similarity": round(shared / union, 4)})
    return {"exact_groups": [group for group in exact.values() if len(group) > 1], "near_pairs": near}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--threshold", type=float, default=.78)
    args = parser.parse_args()
    if not 0 < args.threshold <= 1:
        parser.error("--threshold must be between zero (exclusive) and one (inclusive)")
    sft, rag = read_rows(SFT), read_rows(RAG)
    result = {
        "threshold": args.threshold,
        "sft_prompt": duplicates(sft, "prompt", args.threshold),
        "sft_response": duplicates(sft, "response", args.threshold),
        "rag_text": duplicates(rag, "text", args.threshold),
    }
    write_json(ROOT / "reports/duplicate_audit.json", result)
    print({name: (len(report["exact_groups"]), len(report["near_pairs"]))
           for name, report in result.items() if isinstance(report, dict)})


if __name__ == "__main__":
    main()

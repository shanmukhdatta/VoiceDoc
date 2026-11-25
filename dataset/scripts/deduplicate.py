"""Report exact and character-trigram similarity. Never delete rows automatically."""
import argparse
from collections import Counter, defaultdict
from math import ceil
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


def trigrams(value):
    return {value[j:j + 3] for j in range(len(value) - 2)}


class SimilarityIndex:
    def __init__(self, all_grams, threshold):
        self.frequency = Counter(gram for grams in all_grams for gram in grams)
        self.threshold = threshold
        self.index = defaultdict(list)

    def prefix(self, grams):
        count = len(grams) - ceil(self.threshold * len(grams)) + 1
        return sorted(grams, key=lambda gram: (self.frequency[gram], gram))[:count]

    def similar(self, grams):
        prefix = self.prefix(grams)
        candidates = {i: prior for gram in prefix for i, prior in self.index[gram]}
        for i, prior in sorted(candidates.items()):
            small, large = sorted((len(grams), len(prior)))
            if small / large < self.threshold:
                continue
            shared = len(grams & prior)
            score = shared / (len(grams) + len(prior) - shared)
            if score >= self.threshold:
                yield i, score

    def add(self, i, grams):
        for gram in self.prefix(grams):
            self.index[gram].append((i, grams))


def duplicates(rows, field, threshold=.78):
    exact = defaultdict(list)
    near = []
    ids = [row.get("id") or row["chunk_id"] for row in rows]
    values = [item_text(row, field) for row in rows]
    all_grams = [trigrams(value) for value in values]
    index = SimilarityIndex(all_grams, threshold)
    for i, (value, grams) in enumerate(zip(values, all_grams)):
        exact[value].append(ids[i])
        for prior, score in index.similar(grams):
            if value != values[prior]:
                near.append({"ids": [ids[prior], ids[i]], "similarity": round(score, 4)})
        index.add(i, grams)
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

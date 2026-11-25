"""Select English RAG candidates from an extracted MedlinePlus Health Topic snapshot."""
import argparse
from collections import Counter, defaultdict
from pathlib import Path
import re

from common import ROOT, digest, read_json, read_rows, write, write_json
from deduplicate import SimilarityIndex, normalized, trigrams

AUTHORING = ROOT / "sources/authoring"
REGISTRY = ROOT / "sources/source_registry.json"
LICENSES = ROOT / "sources/license_manifest.json"
ENGLISH_TARGET = 1500
WORD_MIN = 65
WORD_MAX = 185
DOMAIN_GROUPS = {
    "medication": {"Drug Therapy"},
    "hygiene": {"Mouth and Teeth"},
    "maternal": {"Pregnancy and Reproduction", "Children and Teenagers",
                 "Female Reproductive System"},
    "prevention": {"Diagnostic Tests and Screenings", "Wellness and Lifestyle",
                   "Personal Health Issues"},
    "diabetes": {"Diabetes Mellitus", "Endocrine System", "Metabolic Problems"},
    "mental": {"Mental Health and Behavior", "Substance Use and Disorders"},
    "first_aid": {"Injuries and Wounds", "Safety Issues",
                  "Poisoning, Toxicology, Environmental Health"},
    "cardio": {"Blood, Heart and Circulation"},
    "musculoskeletal": {"Bones, Joints and Muscles", "Fitness and Exercise"},
    "digestive": {"Digestive System"},
    "respiratory": {"Lungs and Breathing", "Ear, Nose and Throat"},
    "skin": {"Skin, Hair and Nails"},
    "nutrition": {"Food and Nutrition"},
    "illness": {"Infections", "Immune System", "Brain and Nerves",
                "Kidneys and Urinary System", "Eyes and Vision", "Symptoms"},
}
EMERGENCY_TOPICS = {
    "bleeding", "cardiacarrest", "carbonmonoxidepoisoning", "heatillness",
    "drowning", "choking", "emergencymedicalservices", "shock", "traumaticbraininjury",
}
SENTENCES = re.compile(r"(?<!\b[A-Z]\.)(?<!\bDr\.)(?<!\bSt\.)(?<=[.!?])\s+(?=[A-Z])")
ATTRIBUTION = re.compile(r"\s+NIH:\s+National Institute[^.]*$")


def archive_domain(row):
    if row["source_id"].removeprefix("nlm-") in EMERGENCY_TOPICS:
        return "emergency"
    groups = set(row["source_groups"])
    return next((domain for domain, names in DOMAIN_GROUPS.items() if names & groups), None)


def passages(summary):
    summary = ATTRIBUTION.sub("", summary)
    sentences = [s.strip() for s in SENTENCES.split(summary) if s.strip()]
    pending = []
    words = 0
    for sentence in sentences:
        n = len(sentence.split())
        if words + n > WORD_MAX:
            if WORD_MIN <= words:
                yield " ".join(pending)
            pending, words = [], 0
        if n <= WORD_MAX:
            pending.append(sentence)
            words += n
    if WORD_MIN <= words:
        yield " ".join(pending)


def archive_candidates(evidence, old_rows, count, authored_passages):
    blocked = {row["source_id"] for row in old_rows}
    seen = set()
    layers = defaultdict(list)
    for row in sorted(evidence, key=lambda entry: entry["source_id"]):
        domain = archive_domain(row)
        if not domain or row["source_id"] in blocked:
            continue
        if digest(row["summary_text"]) != row["content_sha256"]:
            raise ValueError(f"{row['source_id']}: archive summary hash mismatch")
        for i, passage in enumerate(passages(row["summary_text"]), 1):
            if any(char in passage for char in "|\r\n"):
                continue
            key = normalized(passage)
            if key in seen:
                continue
            seen.add(key)
            layers[i].append((row, domain, i, passage))
    candidates = [item for layer in sorted(layers) for item in layers[layer]]
    original = [trigrams(normalized(p)) for p in authored_passages]
    fingerprints = [trigrams(normalized(item[3])) for item in candidates]
    index = SimilarityIndex(original + fingerprints, .78)
    for i, grams in enumerate(original):
        index.add(i, grams)
    selected = []
    rejected = 0
    for i, (item, grams) in enumerate(zip(candidates, fingerprints), len(original)):
        if next(index.similar(grams), None) is not None:
            rejected += 1
            continue
        index.add(i, grams)
        selected.append(item)
        if len(selected) == count:
            return selected, rejected
    raise ValueError("The archive yielded too few distinct in-scope English candidates")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    existing = read_json(REGISTRY)
    current = [row for row in existing if row.get("source_archive") != "medlineplus-health-topics"]
    rows = read_rows(args.evidence)
    if len({row["snapshot_sha256"] for row in rows}) != 1:
        raise ValueError("Expected summaries from one pinned MedlinePlus snapshot")
    authored = (AUTHORING / "rag.tsv").read_text(encoding="utf-8").splitlines()
    chosen, rejected = archive_candidates(
        rows, current, ENGLISH_TARGET - len(authored),
        [line.split("|")[1] for line in authored],
    )
    licenses = read_json(LICENSES)
    rights = {k: current[0][k] for k in ("license", "license_evidence", "permitted_usage", "rights_verified")}
    registry = {}
    corpus = []
    for row, domain, index, passage in chosen:
        source_id = row["source_id"]
        slug = source_id.removeprefix("nlm-")
        registry[source_id] = {
            **{k: v for k, v in row.items() if k != "summary_text"}, **rights, "domain": domain,
            "source_archive": "medlineplus-health-topics",
        }
        corpus.append(f"{slug}@archive-{index:02d}|{passage}")
    full_registry = current + sorted(registry.values(), key=lambda row: row["source_id"])
    licenses["source_ids"] = [row["source_id"] for row in full_registry]
    licenses["scope"] = (
        "Only designated MedlinePlus Health Topics summaries. Includes original factual "
        "restatements, synthetic conversations and English extracts from the publisher's "
        "Health Topic XML. Excludes images, linked works and Medical Encyclopedia content. "
        "The public-domain designation is stated under U.S. law."
    )
    write_json(REGISTRY, full_registry)
    write_json(LICENSES, licenses)
    write(AUTHORING / "rag_archive_en.tsv", "".join(line + "\n" for line in corpus))
    write_json(ROOT / "reports/archive_rag_manifest.json", {
        "archive_url": "https://medlineplus.gov/xml.html",
        "archive_snapshot_sha256": rows[0]["snapshot_sha256"],
        "new_english_chunks": len(corpus), "source_topics": len(registry),
        "trigram_near_duplicates_removed": rejected,
        "method": "Extractive, non-overlapping sentences from the public-domain English Summary fields",
        "word_bounds": [WORD_MIN, WORD_MAX], "counts_by_domain": Counter(x[1] for x in chosen),
    })
    print(f"Prepared {len(corpus)} additional English candidate chunks from {len(registry)} topics.")


if __name__ == "__main__":
    main()

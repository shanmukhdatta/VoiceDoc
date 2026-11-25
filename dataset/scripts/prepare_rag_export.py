"""Emit repository-shaped candidate JSON or approved source-level JSON."""
import argparse
from collections import defaultdict
from pathlib import Path
import sys

from common import ROOT, RAG, approval_digest, digest, read_rows, require_approved, sources, write_json


def build_sources(chunks):
    by_source = defaultdict(list)
    for chunk in chunks:
        by_source[chunk["source_id"], chunk["language"]].append(chunk)
    output = []
    for (source, lang), group in sorted(by_source.items()):
        group.sort(key=lambda row: row["chunk_id"])
        first = group[0]
        invariant = ("title", "publisher", "license", "rights_verified", "clinician_reviewed", "reviewed_by")
        if any(any(row[key] != first[key] for key in invariant) for row in group):
            raise ValueError(f"{source}/{lang}: source chunks need consistent metadata and reviewer")
        text = "\n\n".join(row["text"] for row in group)
        output.append({
            **{key: first[key] for key in invariant},
            "source_id": f"{source}-{lang}", "registry_source_id": source, "language": lang,
            "source_url": first["source_url"], "publication_date": first["publication_date"],
            "section_locators": sorted({row["section_locator"] for row in group}),
            "chunk_ids": [row["chunk_id"] for row in group], "text": text,
            "content_sha256": digest(text),
        })
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--candidate", action="store_true")
    mode.add_argument("--reviewed", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    chunks = read_rows(RAG)
    if args.candidate:
        if any(row["clinician_reviewed"] is not False or row["reviewed_by"] is not None for row in chunks):
            raise ValueError("Candidate export requires unreviewed inputs")
        if args.output.suffix != ".json" or "candidate" not in args.output.stem:
            raise ValueError("Use a JSON filename containing 'candidate'")
    else:
        sys.path.insert(0, str(ROOT.parent))
        from rag.ingestion.build_pack import validate
        baseline = {row["chunk_id"]: row for row in chunks}
        registry = sources()
        chunks = read_rows(args.reviewed)
        seen = set()
        for row in chunks:
            key = row["chunk_id"]
            require_approved(row, registry)
            if key in seen or key not in baseline or row["approval"]["record_sha256"] != approval_digest(baseline[key]):
                raise ValueError(f"{key}: duplicate, unknown, or changed candidate")
            seen.add(key)
        if not chunks:
            raise ValueError("No approved chunks were supplied")
    result = build_sources(chunks)
    if args.reviewed:
        for source in result:
            validate(source)
    write_json(args.output, result)
    print(f"{len(result)} source-language entries")


if __name__ == "__main__":
    main()

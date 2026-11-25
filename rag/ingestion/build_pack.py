#!/usr/bin/env python3
"""Build a reproducible *offline* SQLite knowledge pack from explicitly approved JSON.

DO NOT ingest real clinical sources unless rights AND qualified review are documented.
Never uses networking or automatically upgrades a source's rights/review status.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sqlite3
import tempfile
from pathlib import Path

REQUIRED = {
    "source_id", "publisher", "title", "license", "language",
    "rights_verified", "clinician_reviewed", "reviewed_by",
    "text", "content_sha256",
}


def validate(source: dict) -> None:
    missing = REQUIRED.difference(source)
    if missing:
        raise ValueError(f"missing source fields: {sorted(missing)}")
    if source["language"] not in {"en", "te"}:
        raise ValueError("English/Telugu only")
    if source["rights_verified"] is not True or source["clinician_reviewed"] is not True:
        raise ValueError("rights and clinician review must both be explicitly confirmed")
    for key in ("source_id", "publisher", "title", "license", "reviewed_by"):
        if not isinstance(source[key], str) or not source[key].strip():
            raise ValueError(f"invalid {key}")
    if not isinstance(source["text"], str) or not source["text"].strip():
        raise ValueError("missing source text")
    if not isinstance(source["content_sha256"], str):
        raise ValueError("invalid content hash")
    observed = hashlib.sha256(source["text"].encode("utf-8")).hexdigest()
    if observed != source["content_sha256"].lower():
        raise ValueError("source content hash mismatch")


def build(input_file: Path, output_file: Path) -> dict:
    entries = json.loads(input_file.read_text(encoding="utf-8"))
    if not isinstance(entries, list):
        raise ValueError("input must be a list of sources")
    if not entries:
        raise ValueError("refusing to build an empty knowledge pack")
    if output_file.exists():
        raise FileExistsError("refusing to overwrite an existing indexed corpus")
    if output_file.resolve() == input_file.resolve():
        raise ValueError("input and output must differ")
    ids = set()
    for source in entries:
        if not isinstance(source, dict):
            raise ValueError("source must be a JSON object")
        validate(source)
        if source["source_id"] in ids:
            raise ValueError("duplicate source_id")
        ids.add(source["source_id"])

    output_file.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{output_file.name}.", suffix=".staging", dir=output_file.parent
    )
    os.close(descriptor)
    staging_file = Path(temporary_name)
    connection = sqlite3.connect(staging_file)
    try:
        connection.executescript("""
            PRAGMA foreign_keys = ON;
            CREATE TABLE sources (
                source_id TEXT PRIMARY KEY,
                publisher TEXT NOT NULL,
                title TEXT NOT NULL,
                license TEXT NOT NULL,
                language TEXT NOT NULL CHECK(language IN ('en','te')),
                reviewed_by TEXT NOT NULL,
                sha256 TEXT NOT NULL
            );
            CREATE TABLE chunks (
                chunk_id TEXT PRIMARY KEY,
                source_id TEXT NOT NULL REFERENCES sources(source_id),
                language TEXT NOT NULL,
                content TEXT NOT NULL
            );
            CREATE VIRTUAL TABLE chunks_fts USING fts5(chunk_id UNINDEXED, content, tokenize='unicode61');
        """)
        for source in sorted(entries, key=lambda item: item["source_id"]):
            connection.execute(
                "INSERT INTO sources VALUES (?,?,?,?,?,?,?)",
                (source["source_id"], source["publisher"], source["title"],
                 source["license"], source["language"], source["reviewed_by"],
                 source["content_sha256"]),
            )
            text = " ".join(source["text"].split())
            # Character-bounded deterministic chunks; future semantic chunking needs clinician evaluation.
            for index, start in enumerate(range(0, len(text), 800), start=1):
                chunk = text[start:start + 800]
                chunk_id = f"{source['source_id']}:{index:04d}"
                connection.execute("INSERT INTO chunks VALUES (?,?,?,?)",
                                   (chunk_id, source["source_id"], source["language"], chunk))
                connection.execute("INSERT INTO chunks_fts(chunk_id,content) VALUES (?,?)",
                                   (chunk_id, chunk))
        connection.commit()
    except BaseException:
        connection.close()
        staging_file.unlink(missing_ok=True)
        raise
    else:
        connection.close()

    # Hard-link publication is atomic and fails if another process created the target.
    # Staging is in the same directory, so it stays on the same filesystem.
    try:
        os.link(staging_file, output_file)
    except FileExistsError:
        staging_file.unlink(missing_ok=True)
        raise FileExistsError("refusing to overwrite an existing indexed corpus")
    except OSError:
        staging_file.unlink(missing_ok=True)
        raise
    staging_file.unlink()
    digest = hashlib.sha256(output_file.read_bytes()).hexdigest()
    return {"source_count": len(entries), "sha256": digest, "bytes": output_file.stat().st_size}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="licensed and reviewed JSON list")
    parser.add_argument("--output", type=Path, required=True, help="new SQLite file (refuses overwrite)")
    args = parser.parse_args()
    report = build(args.input, args.output)
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Verify local model artifacts against a trusted, manually reviewed manifest.

A manifest claiming approval is not a legal review/signature. Obtain the trusted
SHA256/revision/license from authoritative publisher records independently.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

REQUIRED = {"id", "path", "sha256", "bytes", "format", "source", "revision",
            "license", "redistribution_reviewed", "languages"}
ALLOWED_FORMATS = {"litertlm", "onnx", "ggml", "gguf", "bin"}


def check_record(record: dict, root: Path) -> dict:
    missing = REQUIRED - record.keys()
    if missing:
        raise ValueError(f"Missing required model fields: {sorted(missing)}")
    for field in ["id", "path", "source", "revision", "license"]:
        if not isinstance(record[field], str) or not record[field].strip():
            raise ValueError(f"Invalid {field}")
    if type(record["redistribution_reviewed"]) is not bool or not record["redistribution_reviewed"]:
        raise ValueError("Explicit artifact-specific legal approval required")
    if record["format"] not in ALLOWED_FORMATS:
        raise ValueError("Unsupported runtime format")
    if not isinstance(record["languages"], list) or not record["languages"] or any(
        value not in ("en", "te", "multi") for value in record["languages"]
    ):
        raise ValueError("Only English and Telugu are in scope")
    if type(record["bytes"]) is not int or record["bytes"] < 1:
        raise ValueError("Invalid byte count")
    if not re.fullmatch("[0-9a-fA-F]{64}", record["sha256"]):
        raise ValueError("Invalid SHA-256")
    base = root.resolve()
    artifact = (base / record["path"]).resolve()
    if artifact == base or not artifact.is_relative_to(base):
        raise ValueError("Artifact path escapes model directory")
    if not artifact.is_file():
        raise FileNotFoundError(artifact)
    if artifact.stat().st_size != record["bytes"]:
        raise ValueError("Model artifact size mismatch")
    digest = hashlib.sha256()
    with artifact.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    if digest.hexdigest() != record["sha256"].lower():
        raise ValueError("Model artifact digest mismatch")
    return {"id": record["id"], "status": "BYTE_INTEGRITY_CHECKED_NOT_CLINICALLY_VALIDATED",
            "sha256": digest.hexdigest(), "bytes": artifact.stat().st_size}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    records = json.loads(args.manifest.read_text(encoding="utf-8"))
    if not isinstance(records, list):
        raise ValueError("Expected a JSON array of model records")
    ids = [r["id"] for r in records]
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate model identifiers")
    print(json.dumps([check_record(r, args.root) for r in records], indent=2))


if __name__ == "__main__":
    main()

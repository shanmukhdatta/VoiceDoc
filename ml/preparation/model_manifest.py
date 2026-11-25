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
ALLOWED_FORMATS = {"litertlm", "onnx", "ggml", "gguf", "sqlite", "bin"}
PACK_REQUIRED = {"schema_version", "pack_id", "version", "kind", "runtime",
                 "languages", "assets"}
PACK_KINDS = {"generation", "embedding", "speech", "knowledge"}
PACK_ROLES = {
    "generation": {"model", "tokenizer"},
    "embedding": {"model", "tokenizer"},
    "speech": {"model"},
    "knowledge": {"index"},
}
ASSET_ROLES = {"model", "tokenizer", "vocabulary", "index", "config"}


def check_record(record: dict, root: Path) -> dict:
    if not isinstance(record, dict):
        raise ValueError("Model asset record must be an object")
    missing = REQUIRED - record.keys()
    if missing:
        raise ValueError(f"Missing required model fields: {sorted(missing)}")
    for field in ["id", "path", "source", "revision", "license"]:
        if not isinstance(record[field], str) or not record[field].strip():
            raise ValueError(f"Invalid {field}")
    if type(record["redistribution_reviewed"]) is not bool or not record["redistribution_reviewed"]:
        raise ValueError("Explicit artifact-specific legal approval required")
    if not isinstance(record["format"], str) or record["format"] not in ALLOWED_FORMATS:
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


def check_pack(pack: dict, root: Path, expected_runtime: str | None = None,
               expected_runtime_version: str | None = None,
               expected_embedding_dimension: int | None = None) -> dict:
    """Verify a v1 multi-file model pack's declared compatibility and file bytes.

    This is integrity/compatibility validation only. A JSON manifest cannot prove publisher
    authenticity, license permission, clinical suitability, or real runtime behavior.
    """
    if not isinstance(pack, dict):
        raise ValueError("Pack manifest must be a JSON object")
    missing = PACK_REQUIRED - pack.keys()
    if missing:
        raise ValueError(f"Missing required pack fields: {sorted(missing)}")
    if type(pack["schema_version"]) is not int or pack["schema_version"] != 1:
        raise ValueError("Unsupported model pack schema_version")
    for field in ("pack_id", "version"):
        if not isinstance(pack[field], str) or not pack[field].strip():
            raise ValueError(f"Invalid pack {field}")
    kind = pack["kind"]
    if not isinstance(kind, str):
        raise ValueError("Unsupported pack kind")
    if kind not in PACK_KINDS:
        raise ValueError("Unsupported pack kind")

    runtime = pack["runtime"]
    if not isinstance(runtime, dict) or not all(
        isinstance(runtime.get(field), str) and runtime[field].strip()
        for field in ("name", "version")
    ):
        raise ValueError("Runtime name and version are required")
    if expected_runtime is not None and runtime["name"] != expected_runtime:
        raise ValueError("Incompatible model runtime")
    if expected_runtime_version is not None and runtime["version"] != expected_runtime_version:
        raise ValueError("Incompatible model runtime version")

    languages = pack["languages"]
    if not isinstance(languages, list) or not languages or any(
        language not in ("en", "te") for language in languages
    ) or len(languages) != len(set(languages)):
        raise ValueError("Pack languages must be a unique non-empty subset of en/te")
    dimension = pack.get("embedding_dimension")
    if kind == "embedding":
        if type(dimension) is not int or dimension <= 0:
            raise ValueError("Embedding pack requires a positive embedding_dimension")
        if expected_embedding_dimension is not None and dimension != expected_embedding_dimension:
            raise ValueError("Incompatible embedding dimension")
    elif dimension is not None:
        raise ValueError("embedding_dimension is only valid for embedding packs")

    assets = pack["assets"]
    if not isinstance(assets, list) or not assets:
        raise ValueError("Pack must declare at least one asset")
    if len(assets) > 32:
        raise ValueError("Pack exceeds the 32-asset safety limit")
    ids, paths, roles = set(), set(), set()
    verified = []
    for asset in assets:
        if not isinstance(asset, dict) or not isinstance(asset.get("role"), str):
            raise ValueError("Every asset requires a role")
        checked = check_record(asset, root)
        if asset["role"] not in ASSET_ROLES:
            raise ValueError("Unsupported asset role")
        if asset["role"] in roles:
            raise ValueError(f"Duplicate asset role: {asset['role']}")
        roles.add(asset["role"])
        if asset.get("id") in ids or asset.get("path") in paths:
            raise ValueError("Duplicate asset id or path")
        ids.add(asset.get("id"))
        paths.add(asset.get("path"))
        asset_languages = set(asset["languages"])
        if not asset_languages.issubset(set(languages)) and not (
            asset_languages == {"multi"} and set(languages) == {"en", "te"}
        ):
            raise ValueError("Asset language declarations exceed pack languages")
        verified.append(checked)
    missing_roles = PACK_ROLES[kind] - roles
    if missing_roles:
        raise ValueError(f"Missing required {kind} asset roles: {sorted(missing_roles)}")
    if kind == "generation" and any(asset["format"] != "litertlm" for asset in assets
                                     if asset["role"] == "model"):
        raise ValueError("Generation model must use the LiteRT-LM format")
    if kind == "embedding" and any(asset["format"] != "onnx" for asset in assets
                                    if asset["role"] == "model"):
        raise ValueError("Embedding model must use the ONNX format")
    if kind == "knowledge" and any(asset["format"] != "sqlite" for asset in assets
                                    if asset["role"] == "index"):
        raise ValueError("Knowledge index must use the SQLite format")
    return {
        "pack_id": pack["pack_id"], "version": pack["version"], "kind": kind,
        "runtime": runtime, "languages": languages,
        "embedding_dimension": dimension,
        "assets": verified,
        "status": "BYTE_INTEGRITY_AND_DECLARED_COMPATIBILITY_CHECKED_NOT_AUTHENTICATED_OR_CLINICALLY_VALIDATED",
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--expected-runtime")
    parser.add_argument("--expected-runtime-version")
    parser.add_argument("--expected-embedding-dimension", type=int)
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    if isinstance(manifest, dict):
        result = check_pack(
            manifest, args.root, expected_runtime=args.expected_runtime,
            expected_runtime_version=args.expected_runtime_version,
            expected_embedding_dimension=args.expected_embedding_dimension,
        )
    elif isinstance(manifest, list):
        # Retain compatibility with old single-file lists; new packs should use schema v1.
        if any((args.expected_runtime, args.expected_runtime_version,
                args.expected_embedding_dimension is not None)):
            raise ValueError("Compatibility checks require a versioned pack object")
        if not manifest:
            raise ValueError("Model manifest cannot be empty")
        ids = [r.get("id") for r in manifest if isinstance(r, dict)]
        if len(ids) != len(manifest) or len(ids) != len(set(ids)):
            raise ValueError("Invalid or duplicate model identifiers")
        result = [check_record(r, args.root) for r in manifest]
    else:
        raise ValueError("Expected a v1 pack object or legacy JSON array")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()

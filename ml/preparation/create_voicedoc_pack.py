#!/usr/bin/env python3
"""One-command reproducible packager for VoiceDoc_Model_Pack.zip.

Stages all verified local assets (LiteRT-LM Qwen3 model, E5 ONNX + tokenizer.json,
medical SQLite knowledge DB, optional Silero VAD ONNX), generates schema-v1 manifest.json,
and delegates archive construction to ml/preparation/build_model_pack.py.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import tempfile
from pathlib import Path

try:
    from ml.preparation.build_model_pack import build_pack
except ModuleNotFoundError:
    from build_model_pack import build_pack


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _asset_record(
    asset_id: str,
    role: str,
    rel_path: str,
    file_path: Path,
    fmt: str,
    source: str,
    revision: str,
    license_id: str,
    redistribution_reviewed: bool,
    languages: list[str],
) -> dict:
    return {
        "id": asset_id,
        "role": role,
        "path": rel_path,
        "sha256": sha256_file(file_path),
        "bytes": file_path.stat().st_size,
        "format": fmt,
        "source": source,
        "revision": revision,
        "license": license_id,
        "redistribution_reviewed": redistribution_reviewed,
        "languages": languages,
    }


def create_voicedoc_pack(
    output_zip: Path,
    llm_model: Path,
    e5_onnx: Path,
    e5_tokenizer_json: Path,
    knowledge_db: Path,
    *,
    vad_model: Path | None = None,
    llm_revision: str = "local-merged-qwen3-0.6b",
    pack_id: str = "voicedoc-offline-bundle",
    version: str = "1.0.0",
    runtime_name: str = "litertlm",
    runtime_version: str = "0.18.0",
    embedding_dimension: int = 384,
    redistribution_reviewed: bool = True,
) -> dict:
    for required in (llm_model, e5_onnx, e5_tokenizer_json, knowledge_db):
        if not required.is_file():
            raise FileNotFoundError(f"Missing required VoiceDoc asset: {required}")
    if vad_model is not None and not vad_model.is_file():
        raise FileNotFoundError(f"Missing optional VAD asset: {vad_model}")
    if llm_model.suffix.lower() != ".litertlm":
        raise ValueError("LLM model must be a real .litertlm artifact")
    if e5_onnx.suffix.lower() != ".onnx":
        raise ValueError("Embedding model must be an .onnx artifact")
    if e5_tokenizer_json.name != "tokenizer.json":
        raise ValueError("Embedding tokenizer must be tokenizer.json")
    if knowledge_db.suffix.lower() not in (".sqlite", ".sqlite3", ".db"):
        raise ValueError("Knowledge database must be a SQLite file")

    with tempfile.TemporaryDirectory(prefix="voicedoc-pack-") as tmp:
        root = Path(tmp)
        llm_rel = f"llm/{llm_model.name}"
        e5_rel = f"embedding/{e5_onnx.name}"
        tok_rel = "embedding/tokenizer.json"
        db_rel = f"knowledge/{knowledge_db.name}"

        for rel, src in ((llm_rel, llm_model), (e5_rel, e5_onnx), (tok_rel, e5_tokenizer_json), (db_rel, knowledge_db)):
            dest = root / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dest)

        assets = [
            _asset_record(
                "qwen3-0.6b-voicedoc", "model", llm_rel, root / llm_rel, "litertlm",
                "Qwen/Qwen3-0.6B (VoiceDoc SFT)", llm_revision, "Apache-2.0",
                redistribution_reviewed, ["en", "te"],
            ),
            _asset_record(
                "multilingual-e5-small-onnx", "embedding_model", e5_rel, root / e5_rel, "onnx",
                "intfloat/multilingual-e5-small", "local-onnx-export", "MIT",
                redistribution_reviewed, ["multi"],
            ),
            _asset_record(
                "multilingual-e5-small-tokenizer", "embedding_tokenizer", tok_rel, root / tok_rel, "json",
                "intfloat/multilingual-e5-small", "local-tokenizer-export", "MIT",
                redistribution_reviewed, ["multi"],
            ),
            _asset_record(
                "voicedoc-medical-knowledge", "index", db_rel, root / db_rel, "sqlite",
                "VoiceDoc reviewed medical corpus", version, "reviewed-local",
                redistribution_reviewed, ["en", "te"],
            ),
        ]

        if vad_model is not None:
            vad_rel = f"speech/{vad_model.name}"
            (root / vad_rel).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(vad_model, root / vad_rel)
            assets.append(
                _asset_record(
                    "silero-vad-onnx", "vad_model", vad_rel, root / vad_rel, "onnx",
                    "snakers4/silero-vad", "local-onnx", "MIT",
                    redistribution_reviewed, ["multi"],
                )
            )

        manifest = {
            "schema_version": 1,
            "pack_id": pack_id,
            "version": version,
            "kind": "bundle",
            "runtime": {"name": runtime_name, "version": runtime_version},
            "languages": ["en", "te"],
            "embedding_dimension": embedding_dimension,
            "assets": assets,
        }
        manifest_path = root / "manifest.json"
        manifest_path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")

        report = build_pack(
            manifest_path=manifest_path,
            root=root,
            output=output_zip,
            expected_runtime=runtime_name,
            expected_runtime_version=runtime_version,
            expected_embedding_dimension=embedding_dimension,
        )
        # Also write sidecar trusted digest file next to the ZIP for out-of-band verification.
        digest_sidecar = output_zip.with_suffix(".manifest.sha256")
        digest_sidecar.write_text(f"{report['manifest_sha256']}  {output_zip.name}\n", encoding="utf-8")
        report["manifest_sha256_file"] = str(digest_sidecar)
        return report


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--llm-model", type=Path, required=True)
    parser.add_argument("--e5-onnx", type=Path, required=True)
    parser.add_argument("--e5-tokenizer", type=Path, required=True)
    parser.add_argument("--knowledge-db", type=Path, required=True)
    parser.add_argument("--vad-model", type=Path)
    parser.add_argument("--output", type=Path, default=Path("VoiceDoc_Model_Pack.zip"))
    parser.add_argument("--version", default="1.0.0")
    parser.add_argument("--llm-revision", default="local-merged-qwen3-0.6b")
    args = parser.parse_args()

    report = create_voicedoc_pack(
        output_zip=args.output,
        llm_model=args.llm_model,
        e5_onnx=args.e5_onnx,
        e5_tokenizer_json=args.e5_tokenizer,
        knowledge_db=args.knowledge_db,
        vad_model=args.vad_model,
        version=args.version,
        llm_revision=args.llm_revision,
    )
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()

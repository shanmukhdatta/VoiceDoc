#!/usr/bin/env python3
"""Build an Android-importable schema-v1 ZIP from locally verified model assets.

The manifest digest printed here is an integrity value. It does not establish publisher
authenticity, redistribution rights, or clinical quality; distribute trusted metadata separately.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import tempfile
import zipfile
from pathlib import Path

from model_manifest import check_pack

MAX_MANIFEST_BYTES = 256 * 1024
MAX_ASSET_BYTES = 1_500_000_000
MAX_EXPANDED_BYTES = 2_500_000_000


def build_pack(
    manifest_path: Path,
    root: Path,
    output: Path,
    expected_runtime: str,
    expected_runtime_version: str,
    expected_embedding_dimension: int | None = None,
) -> dict:
    if manifest_path.stat().st_size not in range(1, MAX_MANIFEST_BYTES + 1):
        raise ValueError("Manifest exceeds the Android importer size limit")
    manifest_bytes = manifest_path.read_bytes()
    pack = json.loads(manifest_bytes.decode("utf-8"))
    if not isinstance(pack, dict):
        raise ValueError("Android pack output requires a schema-v1 object manifest")
    check_pack(
        pack,
        root,
        expected_runtime=expected_runtime,
        expected_runtime_version=expected_runtime_version,
        expected_embedding_dimension=expected_embedding_dimension,
    )
    if output.suffix.lower() != ".zip":
        raise ValueError("Output must use the .zip extension")
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        raise FileExistsError(f"Refusing to overwrite {output}")

    root_path = root.resolve()
    files = [("manifest.json", None)]
    expanded_bytes = len(manifest_bytes)
    for asset in pack["assets"]:
        asset_path = (root_path / asset["path"]).resolve()
        if asset_path == root_path or not asset_path.is_relative_to(root_path):
            raise ValueError(f"Asset path escapes root: {asset['path']}")
        asset_bytes = asset_path.stat().st_size
        if not 1 <= asset_bytes <= MAX_ASSET_BYTES:
            raise ValueError(f"Asset exceeds the Android importer size limit: {asset['path']}")
        expanded_bytes += asset_bytes
        if expanded_bytes > MAX_EXPANDED_BYTES:
            raise ValueError("Pack exceeds the Android importer expanded-size limit")
        files.append((asset["path"], asset_path))

    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{output.name}.", suffix=".staging", dir=output.parent
    )
    os.close(descriptor)
    temporary = Path(temporary_name)
    try:
        with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_STORED) as archive:
            for relative_path, file_path in files:
                info = zipfile.ZipInfo(relative_path, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_STORED
                info.create_system = 3
                info.external_attr = (0o100600 & 0xFFFF) << 16
                with archive.open(info, "w") as target:
                    if file_path is None:
                        target.write(manifest_bytes)
                    else:
                        with file_path.open("rb") as source:
                            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                                target.write(chunk)
        # Same-directory hard-link publication is atomic and cannot overwrite a concurrent output.
        os.link(temporary, output)
        digest = hashlib.sha256(manifest_bytes).hexdigest()
        return {
            "output": str(output),
            "pack_id": pack["pack_id"],
            "version": pack["version"],
            "asset_count": len(pack["assets"]),
            "archive_bytes": output.stat().st_size,
            "manifest_sha256": digest,
            "integrity_only_not_authenticated": True,
        }
    finally:
        temporary.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-runtime", required=True)
    parser.add_argument("--expected-runtime-version", required=True)
    parser.add_argument("--expected-embedding-dimension", type=int)
    args = parser.parse_args()
    print(json.dumps(build_pack(
        args.manifest,
        args.root,
        args.output,
        args.expected_runtime,
        args.expected_runtime_version,
        args.expected_embedding_dimension,
    ), indent=2))


if __name__ == "__main__":
    main()

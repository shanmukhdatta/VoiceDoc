#!/usr/bin/env python3
"""Batch-embed queries or passages using an exported multilingual-E5-small ONNX model."""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Sequence

EXPECTED_DIM = 384


def l2_normalize(vector: Sequence[float]) -> list[float]:
    norm = math.sqrt(sum(float(v) * float(v) for v in vector))
    if norm <= 0.0 or not math.isfinite(norm):
        raise ValueError("Cannot L2-normalize zero or non-finite vector")
    return [float(v) / norm for v in vector]


def mean_pool_and_normalize(last_hidden_state, attention_mask):
    import numpy as np

    weights = attention_mask[..., None].astype(np.float32)
    summed = (last_hidden_state * weights).sum(axis=1)
    counts = np.clip(weights.sum(axis=1), 1.0, None)
    pooled = summed / counts
    norms = np.clip(np.linalg.norm(pooled, axis=1, keepdims=True), 1e-12, None)
    normalized = pooled / norms
    if normalized.shape[-1] != EXPECTED_DIM:
        raise ValueError(f"Expected {EXPECTED_DIM} dimensions, got {normalized.shape[-1]}")
    return normalized


def embed_texts(
    onnx_path: Path,
    tokenizer_dir: Path,
    texts: Sequence[str],
    prefix: str = "passage: ",
    batch_size: int = 32,
    max_length: int = 512,
) -> list[list[float]]:
    if prefix not in ("query: ", "passage: "):
        raise ValueError("E5 prefix must be 'query: ' or 'passage: '")
    if not texts:
        return []
    import numpy as np
    import onnxruntime as ort
    from transformers import AutoTokenizer

    tokenizer = AutoTokenizer.from_pretrained(
        str(tokenizer_dir), use_fast=True, local_files_only=True, trust_remote_code=False
    )
    session = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    vectors: list[list[float]] = []
    for start in range(0, len(texts), batch_size):
        batch = [f"{prefix}{t.strip()}" for t in texts[start:start + batch_size]]
        encoded = tokenizer(
            batch, return_tensors="np", padding=True, truncation=True, max_length=max_length
        )
        ort_out = session.run(
            ["last_hidden_state"],
            {
                "input_ids": encoded["input_ids"].astype(np.int64),
                "attention_mask": encoded["attention_mask"].astype(np.int64),
            },
        )[0]
        normalized = mean_pool_and_normalize(ort_out, encoded["attention_mask"])
        vectors.extend(normalized.tolist())
    return vectors


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--onnx", type=Path, required=True)
    parser.add_argument("--tokenizer-dir", type=Path, required=True)
    parser.add_argument("--input-jsonl", type=Path, required=True)
    parser.add_argument("--output-json", type=Path, required=True)
    parser.add_argument("--prefix", choices=("query: ", "passage: "), default="passage: ")
    parser.add_argument("--text-field", default="content")
    parser.add_argument("--id-field", default="chunk_id")
    args = parser.parse_args()

    rows = [json.loads(line) for line in args.input_jsonl.read_text(encoding="utf-8").splitlines() if line.strip()]
    texts = [row[args.text_field] for row in rows]
    ids = [row[args.id_field] for row in rows]
    embeddings = embed_texts(args.onnx, args.tokenizer_dir, texts, prefix=args.prefix)
    result = {item_id: vec for item_id, vec in zip(ids, embeddings)}
    args.output_json.write_text(json.dumps(result), encoding="utf-8")
    print(json.dumps({"embedded_count": len(result), "dimension": EXPECTED_DIM}, indent=2))


if __name__ == "__main__":
    main()

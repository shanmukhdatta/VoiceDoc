#!/usr/bin/env python3
"""Export exact local multilingual-E5-small encoder graph to ONNX.

Heavy dependencies must be installed explicitly. Python model/tokenizer weights must
already exist locally; this script performs NO network download.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def export(local_model: Path, output: Path) -> None:
    import torch
    from torch import nn
    from transformers import AutoModel, AutoTokenizer

    if not local_model.is_dir():
        raise FileNotFoundError(local_model)
    if output.exists() and any(output.iterdir()):
        raise FileExistsError("Output directory must be empty")
    output.mkdir(parents=True, exist_ok=True)
    tokenizer = AutoTokenizer.from_pretrained(
        str(local_model), use_fast=True, local_files_only=True, trust_remote_code=False
    )
    encoder = AutoModel.from_pretrained(
        str(local_model), local_files_only=True, trust_remote_code=False
    ).eval()

    class LastHiddenState(nn.Module):
        def __init__(self, model):
            super().__init__()
            self.encoder = model
        def forward(self, input_ids, attention_mask):
            return self.encoder(input_ids=input_ids, attention_mask=attention_mask).last_hidden_state

    sentence = tokenizer(
        ["query: general health information", "query: ఆరోగ్య సమాచారం"],
        return_tensors="pt", max_length=64, padding="max_length", truncation=True
    )
    ids = sentence["input_ids"]
    mask = sentence["attention_mask"]
    with torch.no_grad():
        torch.onnx.export(
            LastHiddenState(encoder), (ids, mask), str(output / "encoder.onnx"),
            input_names=["input_ids", "attention_mask"],
            output_names=["last_hidden_state"], opset_version=17,
            dynamic_axes={
                "input_ids": {0: "batch", 1: "sequence"},
                "attention_mask": {0: "batch", 1: "sequence"},
                "last_hidden_state": {0: "batch", 1: "sequence"}
            }
        )
        hidden = encoder(input_ids=ids, attention_mask=mask).last_hidden_state
        weights = mask.unsqueeze(-1).float()
        pooled = (hidden * weights).sum(1) / weights.sum(1).clamp(min=1)
        normalized = torch.nn.functional.normalize(pooled, p=2, dim=1)
    tokenizer.save_pretrained(str(output / "tokenizer"))
    (output / "golden-reference.json").write_text(json.dumps({
        "input_ids": ids.tolist(), "attention_mask": mask.tolist(),
        "normalized_embeddings": normalized.tolist(),
        "dimension": normalized.shape[-1],
        "backend": "PyTorch CPU", "note": "Verify exact Android tokenization and ONNX parity"
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Exported local ONNX graph and tokenizer/golden reference (not validated on Android).")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--local-model", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    export(a.local_model, a.output)


if __name__ == "__main__":
    main()

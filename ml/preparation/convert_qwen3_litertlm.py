#!/usr/bin/env python3
"""Convert a merged Qwen3-0.6B Safetensors checkpoint into an Android-ready quantized artifact.

Primary runtime: LiteRT-LM (`.litertlm`, target litertlm-android 0.18.0) via `litert-torch`.
Documented fallback: llama.cpp GGUF (`.gguf`) via `convert_hf_to_gguf.py` + `llama-quantize`.
Never creates fake `.litertlm` files by renaming other formats.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any

LITERTLM_QUANT_RECIPES = {
    "int4": "dynamic_wi4b32_afp32",
    "int8": "dynamic_wi8_afp32",
}
GGUF_QUANT_RECIPES = {
    "int4": "Q4_K_M",
    "int8": "Q8_0",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_merged_checkpoint(merged_dir: Path, max_context_len: int = 2048) -> dict[str, Any]:
    """Validate that merged_dir is a complete dense Qwen3-0.6B FP16/BF16/FP32 Safetensors checkpoint."""
    if not merged_dir.is_dir():
        raise FileNotFoundError(f"Merged checkpoint directory not found: {merged_dir}")
    config_path = merged_dir / "config.json"
    if not config_path.is_file():
        raise FileNotFoundError(f"Missing config.json in {merged_dir}")
    config = json.loads(config_path.read_text(encoding="utf-8"))
    if config.get("model_type") != "qwen3":
        raise ValueError(f"Expected model_type='qwen3', got {config.get('model_type')!r}")
    if config.get("hidden_size") != 1024 or config.get("num_hidden_layers") != 28:
        raise ValueError("Checkpoint dimensions do not match dense Qwen3-0.6B (hidden=1024, layers=28)")
    max_pos = config.get("max_position_embeddings")
    if isinstance(max_pos, int) and max_context_len > max_pos:
        raise ValueError(f"Requested context length {max_context_len} exceeds checkpoint max_position_embeddings={max_pos}")

    safetensors = sorted(merged_dir.glob("*.safetensors"))
    if not safetensors:
        raise FileNotFoundError(f"No .safetensors weight shards found in {merged_dir}")
    if not (merged_dir / "tokenizer.json").is_file() and not (merged_dir / "vocab.json").is_file():
        raise FileNotFoundError(f"Missing tokenizer files in {merged_dir}")
    tok_cfg_path = merged_dir / "tokenizer_config.json"
    if not tok_cfg_path.is_file():
        raise FileNotFoundError(f"Missing tokenizer_config.json in {merged_dir}")
    tok_cfg = json.loads(tok_cfg_path.read_text(encoding="utf-8"))
    if not tok_cfg.get("chat_template") and not (merged_dir / "chat_template.jinja").is_file():
        raise ValueError("Tokenizer is missing chat_template / chat_template.jinja")

    return {
        "model_type": config["model_type"],
        "hidden_size": config["hidden_size"],
        "num_hidden_layers": config["num_hidden_layers"],
        "max_position_embeddings": max_pos,
        "safetensors_files": [p.name for p in safetensors],
    }


def convert_litertlm(
    merged_dir: Path,
    output_dir: Path,
    quantization: str = "int4",
    context_length: int = 2048,
) -> Path:
    """Convert merged Qwen3 Safetensors to `.litertlm` using official `litert_torch`."""
    if quantization not in LITERTLM_QUANT_RECIPES:
        raise ValueError(f"Unsupported LiteRT-LM quantization: {quantization}")
    recipe = LITERTLM_QUANT_RECIPES[quantization]
    try:
        import litert_torch  # noqa: F401
    except ImportError as exc:
        raise RuntimeError(
            "litert-torch is not installed. Install with: "
            "pip install litert-torch ai-edge-litert ai-edge-quantizer torch transformers"
        ) from exc

    output_dir.mkdir(parents=True, exist_ok=True)
    cmd = [
        sys.executable, "-m", "litert_torch.cli", "export_hf",
        "--hf_model_id", str(merged_dir.resolve()),
        "--output_dir", str(output_dir.resolve()),
        "-q", recipe,
    ]
    subprocess.run(cmd, check=True)
    artifacts = sorted(output_dir.glob("*.litertlm"))
    if len(artifacts) != 1 or artifacts[0].stat().st_size == 0:
        raise RuntimeError(f"Expected exactly one non-empty .litertlm artifact in {output_dir}, found {artifacts}")
    return artifacts[0]


def convert_gguf_fallback(
    merged_dir: Path,
    output_dir: Path,
    llama_cpp_dir: Path,
    quantization: str = "int4",
) -> Path:
    """Documented fallback: convert merged Qwen3 Safetensors to quantized GGUF using llama.cpp."""
    if quantization not in GGUF_QUANT_RECIPES:
        raise ValueError(f"Unsupported GGUF quantization: {quantization}")
    convert_script = llama_cpp_dir / "convert_hf_to_gguf.py"
    quantize_bin = shutil.which("llama-quantize") or str(llama_cpp_dir / "build" / "bin" / "llama-quantize")
    if not convert_script.is_file():
        raise FileNotFoundError(f"Missing llama.cpp convert_hf_to_gguf.py at {convert_script}")
    if not Path(quantize_bin).is_file():
        raise FileNotFoundError(f"Missing llama-quantize binary at {quantize_bin}")

    output_dir.mkdir(parents=True, exist_ok=True)
    fp16_gguf = output_dir / "qwen3-0.6b-f16.gguf"
    quant_recipe = GGUF_QUANT_RECIPES[quantization]
    quant_gguf = output_dir / f"qwen3-0.6b-{quant_recipe.lower()}.gguf"
    subprocess.run(
        [sys.executable, str(convert_script), str(merged_dir), "--outfile", str(fp16_gguf), "--outtype", "f16"],
        check=True,
    )
    subprocess.run(
        [str(quantize_bin), str(fp16_gguf), str(quant_gguf), quant_recipe],
        check=True,
    )
    if not quant_gguf.is_file() or quant_gguf.stat().st_size == 0:
        raise RuntimeError(f"GGUF quantization failed to produce {quant_gguf}")
    fp16_gguf.unlink(missing_ok=True)
    return quant_gguf


def write_conversion_metadata(
    output_dir: Path,
    artifact: Path,
    backend: str,
    quantization: str,
    context_length: int,
    checkpoint_info: dict[str, Any],
) -> Path:
    recipe = LITERTLM_QUANT_RECIPES[quantization] if backend == "litertlm" else GGUF_QUANT_RECIPES[quantization]
    metadata = {
        "backend": backend,
        "target_runtime": "litertlm" if backend == "litertlm" else "llama.cpp",
        "target_runtime_version": "0.18.0" if backend == "litertlm" else "gguf",
        "android_abi": "arm64-v8a",
        "quantization": quantization,
        "quantization_recipe": recipe,
        "context_length": context_length,
        "artifact_name": artifact.name,
        "artifact_bytes": artifact.stat().st_size,
        "artifact_sha256": sha256_file(artifact),
        "checkpoint": checkpoint_info,
        "clinical_quality_claim": False,
    }
    meta_path = output_dir / "conversion-metadata.json"
    meta_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    return meta_path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--merged-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--backend", choices=("litertlm", "gguf"), default="litertlm")
    parser.add_argument("--quantization", choices=("int4", "int8"), default="int4")
    parser.add_argument("--context-length", type=int, default=2048)
    parser.add_argument("--llama-cpp-dir", type=Path, help="Required when --backend gguf")
    parser.add_argument("--validate-only", action="store_true")
    args = parser.parse_args()

    info = validate_merged_checkpoint(args.merged_dir, max_context_len=args.context_length)
    if args.validate_only:
        print(json.dumps({"validated": True, "backend": args.backend, "checkpoint": info}, indent=2))
        return

    if args.backend == "litertlm":
        artifact = convert_litertlm(args.merged_dir, args.output_dir, args.quantization, args.context_length)
    else:
        if not args.llama_cpp_dir:
            parser.error("--llama-cpp-dir is required when --backend gguf")
        artifact = convert_gguf_fallback(args.merged_dir, args.output_dir, args.llama_cpp_dir, args.quantization)

    meta_path = write_conversion_metadata(
        args.output_dir, artifact, args.backend, args.quantization, args.context_length, info
    )
    print(json.dumps({"artifact": str(artifact), "metadata": str(meta_path)}, indent=2))


if __name__ == "__main__":
    main()

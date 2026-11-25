"""One reviewed-data Qwen3 training workflow, with preflight and merge."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from ml.training.train_qlora import load_reviewed_jsonl, check_splits, tokenize_example, sha256_file

def read(path):
    return json.loads(path.read_text(encoding="utf-8"))

def command(*args):
    subprocess.run([sys.executable, *map(str, args)], cwd=ROOT, check=True)

def model_files(model):
    paths = sorted(p for p in model.iterdir() if p.is_file() and
                   p.suffix in (".json", ".safetensors", ".txt", ".model", ".jinja"))
    if not (model / "config.json").is_file() or not any(p.suffix == ".safetensors" for p in paths):
        raise ValueError("Complete local safetensors snapshot required")
    return {p.name: sha256_file(p) for p in paths}

def prepare(reviewed, target, model, cfg):
    command(ROOT / "dataset/scripts/prepare_training_export.py",
            "--reviewed", reviewed, "--output-dir", target)
    from transformers import AutoConfig, AutoTokenizer
    config = AutoConfig.from_pretrained(model, local_files_only=True, trust_remote_code=False)
    if config.model_type != "qwen3" or config.hidden_size != 1024 or config.num_hidden_layers != 28:
        raise ValueError("Expected dense Qwen3-0.6B architecture")
    tokenizer = AutoTokenizer.from_pretrained(model, local_files_only=True, trust_remote_code=False)
    splits = {name: load_reviewed_jsonl(target / f"{name}.jsonl")
              for name in ("train", "validation", "test")}
    check_splits(splits["train"], splits["validation"])
    check_splits(splits["train"] + splits["validation"], splits["test"])
    seen = set()
    summary = {}
    for name, rows in splits.items():
        ids = {row["id"] for row in rows}
        if len(ids) != len(rows) or seen & ids:
            raise ValueError("Duplicate record ID")
        seen |= ids
        lengths = [len(tokenize_example(tokenizer, row, cfg["max_seq_len"])["input_ids"]) for row in rows]
        summary[name] = {"count": len(rows), "max_tokens": max(lengths)}
    return summary

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--reviewed-sft", type=Path, required=True)
    p.add_argument("--base-model-dir", type=Path, required=True)
    p.add_argument("--base-model-revision", required=True)
    p.add_argument("--run-dir", type=Path, required=True)
    p.add_argument("--config", type=Path, default=ROOT / "ml/training/config.json")
    p.add_argument("--resume-from-checkpoint", type=Path)
    p.add_argument("--prepare-only", action="store_true")
    a = p.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", a.base_model_revision):
        p.error("Provide exact 40-character Hugging Face commit SHA")
    a.reviewed_sft = a.reviewed_sft.resolve()
    a.base_model_dir = a.base_model_dir.resolve()
    a.run_dir = a.run_dir.resolve()
    cfg = read(a.config)
    if cfg.pop("model_id") != "Qwen/Qwen3-0.6B":
        p.error("Unexpected model")
    keys = {"max_seq_len", "epochs", "batch_size", "eval_batch_size", "gradient_accumulation",
            "learning_rate", "warmup_ratio", "lora_rank", "lora_alpha", "lora_dropout", "target_modules", "seed"}
    if set(cfg) != keys:
        p.error("Unexpected configuration fields")
    os.environ["HF_HUB_OFFLINE"] = "1"
    os.environ["TRANSFORMERS_OFFLINE"] = "1"
    os.environ["CUDA_VISIBLE_DEVICES"] = "0"
    a.run_dir.mkdir(parents=True, exist_ok=True)
    manifest = a.run_dir / "input-manifest.json"
    approved = a.run_dir / "approved"
    with tempfile.TemporaryDirectory() as d:
        tmp = Path(d)
        counts = prepare(a.reviewed_sft, tmp, a.base_model_dir, cfg)
        state = {"model_revision": a.base_model_revision, "model_files": model_files(a.base_model_dir),
                 "reviewed_sha256": sha256_file(a.reviewed_sft), "configuration": cfg,
                 "splits": counts, "split_sha256": {f.name: sha256_file(f) for f in tmp.glob("*.jsonl")},
                 "code_sha256": {str(f.relative_to(ROOT)): sha256_file(f) for f in (
                     Path(__file__).resolve(), ROOT / "ml/training/train_qlora.py",
                     ROOT / "ml/training/merge_adapter.py", ROOT / "dataset/scripts/prepare_training_export.py")}}
        if manifest.exists():
            if read(manifest) != state:
                raise ValueError("Inputs or code changed; use a fresh run directory")
            for name, digest in state["split_sha256"].items():
                if sha256_file(approved / name) != digest:
                    raise ValueError("Existing export differs from manifest")
        else:
            if any(a.run_dir.iterdir()):
                raise FileExistsError("Use a fresh run directory")
            approved.mkdir()
            for f in tmp.glob("*.jsonl"):
                (approved / f.name).write_bytes(f.read_bytes())
            manifest.write_text(json.dumps(state, indent=2, sort_keys=True), encoding="utf-8")
    print(json.dumps(counts, indent=2))
    if a.prepare_only:
        return
    train = a.run_dir / "training"
    args = ["--train-jsonl", approved / "train.jsonl", "--validation-jsonl", approved / "validation.jsonl",
            "--test-jsonl", approved / "test.jsonl", "--base-model-dir", a.base_model_dir,
            "--base-model-revision", a.base_model_revision, "--output-dir", train]
    for key, value in cfg.items():
        args.extend(["--" + key.replace("_", "-"), str(value)])
    if a.resume_from_checkpoint:
        checkpoint = a.resume_from_checkpoint.resolve()
        if checkpoint.parent != (train / "checkpoints").resolve() or not (checkpoint / "trainer_state.json").is_file():
            p.error("Resume only from a checkpoint inside this run")
        args.extend(["--resume-from-checkpoint", checkpoint])
    elif train.exists() and any(train.iterdir()):
        p.error("Training output exists; resume explicitly or use a fresh run directory")
    command(ROOT / "ml/training/train_qlora.py", *args)
    command(ROOT / "ml/training/merge_adapter.py", "--base-model-dir", a.base_model_dir, "--run-dir", a.run_dir)

if __name__ == "__main__":
    main()

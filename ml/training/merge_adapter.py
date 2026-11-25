"""Merge into original unquantized CPU base and save FP16 safetensors."""
import argparse
import json
from pathlib import Path
import shutil
import tempfile
from train_qlora import sha256_file
from run_kaggle import model_files

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--base-model-dir", type=Path, required=True)
    p.add_argument("--run-dir", type=Path, required=True)
    a = p.parse_args()
    from peft import PeftModel
    from transformers import AutoModelForCausalLM, AutoTokenizer
    import torch
    manifest = json.loads((a.run_dir / "input-manifest.json").read_text())
    train = a.run_dir / "training"
    metadata = json.loads((train / "run-metadata.json").read_text())
    if manifest["model_revision"] != metadata["base_model_revision"] or model_files(a.base_model_dir) != manifest["model_files"]:
        raise ValueError("Base snapshot differs from training manifest")
    for split in ("train", "validation", "test"):
        if metadata[f"{'training' if split == 'train' else split}_data_sha256"] != manifest["split_sha256"][split + ".jsonl"]:
            raise ValueError("Training data hash mismatch")
    adapter = train / "adapter"
    output = a.run_dir / "merged"
    if output.exists():
        raise FileExistsError(output)
    base = AutoModelForCausalLM.from_pretrained(a.base_model_dir, torch_dtype=torch.float32,
                device_map={"": "cpu"}, local_files_only=True, trust_remote_code=False)
    model = PeftModel.from_pretrained(base, adapter, is_trainable=False, local_files_only=True)
    merged = model.merge_and_unload(safe_merge=True).eval().to(torch.float16)
    merged.config.use_cache = True
    tokenizer = AutoTokenizer.from_pretrained(adapter, local_files_only=True, trust_remote_code=False)
    staging = Path(tempfile.mkdtemp(prefix=".merge-", dir=a.run_dir))
    try:
        merged.save_pretrained(staging, safe_serialization=True, max_shard_size="2GB")
        tokenizer.save_pretrained(staging)
        info = {"base_revision": manifest["model_revision"], "dtype": "float16",
                "adapter_sha256": sha256_file(adapter / "adapter_model.safetensors"),
                "files": {f.name: sha256_file(f) for f in staging.iterdir() if f.is_file()},
                "artifact": "merged Hugging Face checkpoint; not a mobile runtime package",
                "clinical_quality_claim": False}
        (staging / "export-manifest.json").write_text(json.dumps(info, indent=2), encoding="utf-8")
        staging.rename(output)
    finally:
        if staging.exists():
            shutil.rmtree(staging)
    print(output)

if __name__ == "__main__":
    main()

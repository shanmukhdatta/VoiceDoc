#!/usr/bin/env python3
"""Train an auditable QLoRA adapter from separately reviewed local JSONL splits.

This does not create a LiteRT-LM artifact, validate clinical quality, or endorse a model.
All model and data files are read locally; this script never downloads a base model or dataset.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
import os
import platform
import subprocess
import sys
from pathlib import Path
from typing import Any

try:
    from ml.preparation.prepare_reviewed_sft import validate
except ModuleNotFoundError:
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "preparation"))
    from prepare_reviewed_sft import validate


SYSTEM_PROMPT = (
    "You provide general health information, not diagnosis or treatment. Use only information "
    "in the user's provided context. State when the context is insufficient. Do not claim to "
    "rule out emergencies. Keep the user's language."
)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_reviewed_jsonl(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
            if not isinstance(row, dict):
                raise ValueError("row must be a JSON object")
            validate(row)
        except Exception as error:
            raise ValueError(f"{path}:{line_number}: {error}") from error
        records.append(row)
    if not records:
        raise ValueError(f"Reviewed dataset is empty: {path}")
    return records


def check_splits(train: list[dict], validation: list[dict]) -> None:
    if not train or not validation:
        raise ValueError("Training and validation splits must both be non-empty")
    train_scenarios = {row["scenario_id"] for row in train}
    validation_scenarios = {row["scenario_id"] for row in validation}
    overlap = train_scenarios & validation_scenarios
    if overlap:
        raise ValueError(f"Scenario leakage between train and validation: {sorted(overlap)[:5]}")
    if {row["language"] for row in train + validation} - {"en", "te"}:
        raise ValueError("Only English and Telugu are in scope")


def tokenize_example(tokenizer: Any, row: dict, max_length: int) -> dict[str, list[int]]:
    """Tokenize prompt + answer and mask the user/prompt portion from the loss."""
    prompt_messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": row["prompt"]},
    ]
    complete_messages = prompt_messages + [{"role": "assistant", "content": row["response"]}]
    prompt_ids = tokenizer.apply_chat_template(
        prompt_messages, tokenize=True, add_generation_prompt=True
    )
    full_ids = tokenizer.apply_chat_template(
        complete_messages, tokenize=True, add_generation_prompt=False
    )
    if not isinstance(prompt_ids, list) or not isinstance(full_ids, list):
        raise ValueError("Tokenizer chat template must return token ID lists")
    if len(full_ids) > max_length:
        raise ValueError(
            f"Reviewed example {row['scenario_id']} has {len(full_ids)} tokens; "
            f"max sequence length is {max_length}. Shorten it explicitly rather than truncating the answer."
        )
    if len(full_ids) <= len(prompt_ids) or full_ids[:len(prompt_ids)] != prompt_ids:
        raise ValueError("Tokenizer chat template does not preserve the generation prompt as a token prefix")
    labels = [-100] * len(prompt_ids) + full_ids[len(prompt_ids):]
    if not any(token != -100 for token in labels):
        raise ValueError("No assistant tokens remain for training")
    return {"input_ids": full_ids, "labels": labels}


def package_version(name: str) -> str | None:
    try:
        return importlib.metadata.version(name)
    except importlib.metadata.PackageNotFoundError:
        return None


def git_revision() -> str | None:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"], text=True, stderr=subprocess.DEVNULL
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return None


def train(args: argparse.Namespace, train_rows: list[dict], validation_rows: list[dict]) -> dict:
    os.environ["HF_HUB_OFFLINE"] = "1"
    os.environ["TRANSFORMERS_OFFLINE"] = "1"
    try:
        import torch
        from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
        from torch.utils.data import Dataset
        from transformers import (
            AutoConfig,
            AutoModelForCausalLM,
            AutoTokenizer,
            BitsAndBytesConfig,
            Trainer,
            TrainingArguments,
        )
    except ImportError as error:
        raise RuntimeError(
            "Install ml/training/requirements-kaggle.txt in a CUDA-enabled Kaggle session"
        ) from error

    if not torch.cuda.is_available():
        raise RuntimeError("QLoRA requires a CUDA GPU; this command will not silently train on CPU")
    if not args.base_model_dir.is_dir() or not (args.base_model_dir / "config.json").is_file():
        raise FileNotFoundError("Base model must be a complete local Transformers model directory")
    if args.output_dir.exists() and any(args.output_dir.iterdir()) and not args.resume_from_checkpoint:
        raise FileExistsError("Output directory is not empty; use a fresh directory or resume explicitly")
    args.output_dir.mkdir(parents=True, exist_ok=True)

    tokenizer = AutoTokenizer.from_pretrained(
        args.base_model_dir, local_files_only=True, trust_remote_code=False
    )
    config = AutoConfig.from_pretrained(
        args.base_model_dir, local_files_only=True, trust_remote_code=False
    )
    position_limit = getattr(config, "max_position_embeddings", None)
    padded_limit = ((args.max_seq_len + args.pad_to_multiple_of - 1) // args.pad_to_multiple_of) * args.pad_to_multiple_of
    if isinstance(position_limit, int) and padded_limit > position_limit:
        raise ValueError(
            f"Padded sequence length {padded_limit} exceeds checkpoint limit {position_limit}"
        )
    if not getattr(tokenizer, "chat_template", None):
        raise ValueError("Base tokenizer has no chat template; do not invent a model prompt format")
    if tokenizer.pad_token_id is None:
        if tokenizer.eos_token_id is None:
            raise ValueError("Tokenizer must provide EOS or PAD token")
        tokenizer.pad_token = tokenizer.eos_token

    class TokenizedRows(Dataset):
        def __init__(self, rows: list[dict]):
            self.items = [tokenize_example(tokenizer, row, args.max_seq_len) for row in rows]
        def __len__(self):
            return len(self.items)
        def __getitem__(self, index):
            return self.items[index]

    class PadAndMask:
        def __call__(self, features):
            length = max(len(item["input_ids"]) for item in features)
            length = ((length + args.pad_to_multiple_of - 1) // args.pad_to_multiple_of) * args.pad_to_multiple_of
            input_ids, attention_mask, labels = [], [], []
            for item in features:
                pad = length - len(item["input_ids"])
                input_ids.append(item["input_ids"] + [tokenizer.pad_token_id] * pad)
                attention_mask.append([1] * len(item["input_ids"]) + [0] * pad)
                labels.append(item["labels"] + [-100] * pad)
            return {
                "input_ids": torch.tensor(input_ids, dtype=torch.long),
                "attention_mask": torch.tensor(attention_mask, dtype=torch.long),
                "labels": torch.tensor(labels, dtype=torch.long),
            }

    precision = torch.bfloat16 if torch.cuda.is_bf16_supported() else torch.float16
    quantization = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_use_double_quant=True,
        bnb_4bit_compute_dtype=precision,
    )
    model = AutoModelForCausalLM.from_pretrained(
        args.base_model_dir,
        config=config,
        local_files_only=True,
        trust_remote_code=False,
        device_map="auto",
        quantization_config=quantization,
        torch_dtype=precision,
    )
    model.config.use_cache = False
    model = prepare_model_for_kbit_training(model, use_gradient_checkpointing=True)
    model = get_peft_model(model, LoraConfig(
        r=args.lora_rank,
        lora_alpha=args.lora_alpha,
        lora_dropout=args.lora_dropout,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules=[module.strip() for module in args.target_modules.split(",") if module.strip()],
    ))

    training_args = TrainingArguments(
        output_dir=str(args.output_dir / "checkpoints"),
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.eval_batch_size,
        gradient_accumulation_steps=args.gradient_accumulation,
        learning_rate=args.learning_rate,
        warmup_ratio=0.03,
        lr_scheduler_type="cosine",
        logging_steps=10,
        eval_strategy="epoch",
        save_strategy="epoch",
        save_total_limit=2,
        load_best_model_at_end=True,
        metric_for_best_model="eval_loss",
        greater_is_better=False,
        gradient_checkpointing=True,
        optim="paged_adamw_8bit",
        bf16=precision == torch.bfloat16,
        fp16=precision == torch.float16,
        report_to="none",
        seed=args.seed,
        data_seed=args.seed,
    )
    runner = Trainer(
        model=model,
        args=training_args,
        train_dataset=TokenizedRows(train_rows),
        eval_dataset=TokenizedRows(validation_rows),
        data_collator=PadAndMask(),
        processing_class=tokenizer,
    )
    runner.train(resume_from_checkpoint=args.resume_from_checkpoint)
    evaluation = runner.evaluate()
    adapter_dir = args.output_dir / "adapter"
    runner.model.save_pretrained(adapter_dir)
    tokenizer.save_pretrained(adapter_dir)
    return {
        "evaluation": evaluation,
        "precision": str(precision),
        "adapter_dir": str(adapter_dir),
        "versions": {name: package_version(name) for name in
                     ("torch", "transformers", "peft", "bitsandbytes", "accelerate")},
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--train-jsonl", type=Path, required=True)
    parser.add_argument("--validation-jsonl", type=Path, required=True)
    parser.add_argument("--base-model-dir", type=Path, required=True)
    parser.add_argument("--base-model-revision", required=True,
                        help="Exact publisher/model revision recorded for this local checkpoint")
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--max-seq-len", type=int, default=2048)
    parser.add_argument("--pad-to-multiple-of", type=int, default=8)
    parser.add_argument("--epochs", type=float, default=2.0)
    parser.add_argument("--batch-size", type=int, default=1)
    parser.add_argument("--eval-batch-size", type=int, default=1)
    parser.add_argument("--gradient-accumulation", type=int, default=8)
    parser.add_argument("--learning-rate", type=float, default=2e-4)
    parser.add_argument("--lora-rank", type=int, default=16)
    parser.add_argument("--lora-alpha", type=int, default=32)
    parser.add_argument("--lora-dropout", type=float, default=0.05)
    parser.add_argument("--target-modules", default="q_proj,k_proj,v_proj,o_proj,gate_proj,up_proj,down_proj")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--resume-from-checkpoint")
    parser.add_argument("--validate-only", action="store_true",
                        help="Validate provenance fields and group split without loading ML packages or using a GPU")
    args = parser.parse_args()
    if args.max_seq_len < 128 or args.max_seq_len > 8192:
        parser.error("--max-seq-len must be between 128 and 8192")
    if args.pad_to_multiple_of < 1 or args.pad_to_multiple_of > 64:
        parser.error("--pad-to-multiple-of must be between 1 and 64")
    if args.epochs <= 0 or args.batch_size < 1 or args.eval_batch_size < 1 or args.gradient_accumulation < 1:
        parser.error("Training duration and batch sizes must be positive")
    if args.learning_rate <= 0 or args.lora_rank < 1 or args.lora_alpha < 1 or not 0 <= args.lora_dropout < 1:
        parser.error("Learning rate/rank/alpha must be positive and LoRA dropout must be in [0, 1)")
    target_modules = [module.strip() for module in args.target_modules.split(",") if module.strip()]
    if not target_modules or len(target_modules) != len(set(target_modules)):
        parser.error("--target-modules must be a non-empty comma-separated list without duplicates")
    train_rows = load_reviewed_jsonl(args.train_jsonl)
    validation_rows = load_reviewed_jsonl(args.validation_jsonl)
    check_splits(train_rows, validation_rows)
    if not args.base_model_revision.strip():
        parser.error("--base-model-revision must identify the exact base checkpoint")
    if args.validate_only:
        print(json.dumps({"train_examples": len(train_rows),
                          "validation_examples": len(validation_rows),
                          "scenario_leakage": False, "gpu_training_started": False,
                          "clinical_quality_claim": False}, indent=2))
        return
    result = train(args, train_rows, validation_rows)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    metadata = {
        "base_model_dir": str(args.base_model_dir.resolve()),
        "base_model_revision": args.base_model_revision,
        "training_data_sha256": sha256_file(args.train_jsonl),
        "validation_data_sha256": sha256_file(args.validation_jsonl),
        "training_examples": len(train_rows),
        "validation_examples": len(validation_rows),
        "scenario_leakage": False,
        "seed": args.seed,
        "max_seq_len": args.max_seq_len,
        "epochs": args.epochs,
        "learning_rate": args.learning_rate,
        "lora_rank": args.lora_rank,
        "lora_alpha": args.lora_alpha,
        "target_modules": target_modules,
        "system_prompt": SYSTEM_PROMPT,
        "git_revision": git_revision(),
        "python": platform.python_version(),
        "training_runtime": result,
        "artifact_boundary": "PEFT adapter only; not a merged model or LiteRT-LM package",
        "clinical_quality_claim": False,
    }
    (args.output_dir / "run-metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps({"adapter_dir": result["adapter_dir"],
                      "eval_loss": result["evaluation"].get("eval_loss"),
                      "clinical_quality_claim": False}, indent=2))


if __name__ == "__main__":
    main()

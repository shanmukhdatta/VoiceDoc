"""Validate candidate invariants and optionally audit with the pinned Qwen tokenizer."""
import argparse
from collections import defaultdict
import importlib
from pathlib import Path
import re
import sys

from common import ROOT, SFT, RAG, CAPABILITIES, STYLES, approval_digest, digest
from common import has_rights, read_json, read_rows, sources, write_json, write_rows
from deduplicate import duplicates

SFT_STRINGS = ("id", "scenario_id", "split_group", "domain", "capability", "language",
               "language_style", "risk_level", "prompt", "response", "source_id", "license")
RAG_STRINGS = ("chunk_id", "source_id", "publisher", "title", "domain", "language", "text",
               "source_url", "section_locator", "license", "content_sha256")
TELUGU = re.compile(r"[\u0C00-\u0C7F]")
LATIN = re.compile(r"[A-Za-z]")
THINK = re.compile(r"</?think\s*>", re.I)
REPOSITORY = ROOT.parent


def language_ok(value, style):
    if style == "telugu":
        return bool(TELUGU.search(value))
    if style == "code":
        return bool(TELUGU.search(value) and LATIN.search(value))
    return bool(LATIN.search(value) and not TELUGU.search(value))


def receipt_queue(rows, kind):
    for row in rows:
        yield {
            "dataset": kind, "record_id": row.get("id") or row["chunk_id"],
            "domain": row["domain"], "language": row["language"],
            "risk_level": row.get("risk_level"), "source_id": row["source_id"],
            "record_sha256": approval_digest(row), "review_status": "pending",
            "priority": 0 if row.get("risk_level") == "high" else 1 if row["language"] == "te" else 2,
        }


def validation(sft, rag, split_rows, registry, manifest):
    errors = []

    def check(condition, key, reason):
        if not condition:
            errors.append({"record_id": key, "reason": reason})

    rag_by_id = {row.get("chunk_id"): row for row in rag}
    sft_by_id = {row.get("id"): row for row in sft}
    check(len(rag_by_id) == len(rag), "rag", "Duplicate chunk identifiers")
    check(len(sft_by_id) == len(sft), "sft", "Duplicate SFT identifiers")
    check(set(registry) == set(manifest["source_ids"]), "sources", "License manifest source mismatch")
    for key, row in registry.items():
        check(has_rights(row) and row["license_evidence"] == manifest["policy_url"]
              and row["url"].startswith("https://medlineplus.gov/")
              and row["section_locator"].startswith("Summary,")
              and bool(re.fullmatch(r"[0-9a-f]{64}", row["content_sha256"])),
              key, "Incomplete scoped rights evidence or source hash")

    def fields(row, strings, key):
        valid = all(isinstance(row.get(field), str) and row[field].strip() for field in strings)
        check(valid, key, "Missing or invalid required text fields")
        check(row.get("rights_verified") is True and row.get("clinician_reviewed") is False
              and "reviewed_by" in row and row["reviewed_by"] is None,
              key, "Candidate rights/review state is incorrect")
        return valid

    for key, row in rag_by_id.items():
        if not fields(row, RAG_STRINGS, key):
            continue
        origin = registry.get(row["source_id"], {})
        check(all(row[target] == origin.get(source) for target, source in (
            ("publisher", "publisher"), ("title", "title"), ("source_url", "url"),
            ("section_locator", "section_locator"), ("domain", "domain"), ("license", "license"),
            ("publication_date", "publication_date"),
        )) if "publication_date" in row else False, key, "RAG source metadata mismatch")
        check(row["content_sha256"] == digest(row["text"]), key, "RAG text hash mismatch")
        check(row["language"] in {"en", "te"} and language_ok(
            row["text"], "english" if row["language"] == "en" else "telugu"),
            key, "RAG script mismatch")
        if row["language"] == "te":
            english = rag_by_id.get(row.get("translation_of"), {})
            check(english.get("language") == "en" and english.get("source_id") == row["source_id"],
                  key, "Translation pair mismatch")
            check(re.findall(r"\d+", english.get("text", "")) == re.findall(r"\d+", row["text"]),
                  key, "Arabic digit sequences differ across a translation pair")

    for key, row in sft_by_id.items():
        if not fields(row, SFT_STRINGS, key):
            continue
        source_ids = row.get("source_ids")
        check(isinstance(source_ids, list) and row["source_id"] in source_ids
              and all(source in registry for source in source_ids)
              and all(row["domain"] == registry[source]["domain"]
                      and row["license"] == registry[source]["license"] for source in source_ids),
              key, "SFT source metadata mismatch")
        check(row.get("synthetic") is True and row["capability"] in CAPABILITIES
              and row["risk_level"] in {"low", "moderate", "high"},
              key, "Invalid synthetic, capability or risk metadata")
        style, prompt, answer = row["language_style"], row["prompt"], row["response"]
        question = prompt.split("\n\n", 1)[-1] if row.get("evidence_chunk_ids") else prompt
        check(STYLES.get(style) == row["language"] and language_ok(question, style)
              and language_ok(answer, style), key, "SFT language/script mismatch")
        check(len(prompt) <= 4000 and len(answer) <= 6000 and not THINK.search(prompt + answer),
              key, "Repository size limit or reasoning markup")
        if row["capability"] == "grounded":
            refs = row.get("evidence_chunk_ids") or []
            check(bool(refs) and all(
                ref in rag_by_id and rag_by_id[ref]["source_id"] in row["source_ids"]
                and rag_by_id[ref]["text"] in prompt for ref in refs
            ), key, "Evidence is missing from prompt or provenance")
        if row["capability"] == "multi":
            turns = row.get("conversation") or []
            check([turn.get("role") for turn in turns] == ["user", "assistant", "user", "assistant"]
                  and turns[-1].get("content") == answer
                  and all(turn.get("content") and turn["content"] in prompt for turn in turns[:-1]),
                  key, "Conversation metadata and flattened transcript disagree")
        if row["risk_level"] == "high" or row["capability"] == "emergency":
            lead = re.split(r"[.!?]", answer, maxsplit=1)[0].lower()
            check(row["risk_level"] == "high" and row["capability"] == "emergency" and (
                ("emergency" in lead and re.search(r"\b(now|immediately)\b", lead))
                or ("వెంటనే" in lead and ("అత్యవసర" in lead or "emergency" in lead))
            ) and "?" not in answer, key, "Emergency candidate did not lead with immediate escalation")

    groups = defaultdict(set)
    scenarios = defaultdict(set)
    membership = []
    for name, rows in split_rows.items():
        for row in rows:
            key = row["id"]
            membership.append(key)
            groups[row["split_group"]].add(name)
            scenarios[row["scenario_id"]].add(name)
            check(row == sft_by_id.get(key), key, "Split payload differs from candidate")
    check(len(membership) == len(set(membership)) and set(membership) == set(sft_by_id),
          "splits", "Splits must partition candidates")
    check(all(len(x) == 1 for x in groups.values()) and all(len(x) == 1 for x in scenarios.values()),
          "splits", "Topic or scenario leaked across splits")
    check(all(split_rows.values()), "splits", "A split is empty")
    expected_domains = set(read_json(ROOT / "taxonomy/medical_domains.json"))
    check({r["domain"] for r in sft} == {r["domain"] for r in rag} == expected_domains,
          "coverage", "A medical domain is missing")
    for kind, rows, field in (("SFT prompt", sft, "prompt"), ("SFT response", sft, "response"),
                              ("RAG text", rag, "text")):
        check(not duplicates(rows, field)["exact_groups"], kind, "Exact normalized duplicates")
    return errors


def token_lengths(sft, rag, model_dir, max_seq_len):
    from transformers import AutoTokenizer
    sys.path.insert(0, str(REPOSITORY))
    tokenize_example = importlib.import_module("ml.training.train_qlora").tokenize_example
    tokenizer = AutoTokenizer.from_pretrained(model_dir, local_files_only=True, trust_remote_code=False)
    if not tokenizer.chat_template:
        raise ValueError("Expected a pinned base-model tokenizer with a chat template")
    sft_lengths = [len(tokenize_example(tokenizer, row, max_seq_len)["input_ids"]) for row in sft]
    rag_lengths = [len(tokenizer(row["text"], add_special_tokens=False)["input_ids"]) for row in rag]
    return {"status": "executed", "sft_min": min(sft_lengths), "sft_max": max(sft_lengths),
            "rag_min": min(rag_lengths), "rag_max": max(rag_lengths),
            "rag_between_100_and_250": sum(100 <= n <= 250 for n in rag_lengths)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path)
    parser.add_argument("--max-seq-len", type=int, default=2048)
    args = parser.parse_args()
    sft, rag = read_rows(SFT), read_rows(RAG)
    splits = {name: read_rows(ROOT / f"sft/splits/{name}.jsonl")
              for name in ("train", "validation", "test")}
    errors = validation(sft, rag, splits, sources(), read_json(ROOT / "sources/license_manifest.json"))
    tokenizer = {"status": "not_run", "reason": "Pinned Qwen tokenizer assets and Transformers unavailable"}
    if args.model_dir and not errors:
        tokenizer = token_lengths(sft, rag, args.model_dir, args.max_seq_len)
    queue = list(receipt_queue(sft, "sft")) + list(receipt_queue(rag, "rag"))
    write_rows(ROOT / "reports/medical_review_queue.jsonl", sorted(
        queue, key=lambda row: (row["priority"], row["record_id"])))
    write_rows(ROOT / "reports/rejected_records.jsonl", errors)
    write_json(ROOT / "reports/validation_results.json", {
        "status": "failed" if errors else "passed", "record_errors": len(errors),
        "sft_checked": len(sft), "rag_checked": len(rag), "clinical_reviewed": 0,
        "tokenizer": tokenizer, "semantic_verification": "requires qualified review",
    })
    print(f"Checked {len(sft)} SFT and {len(rag)} RAG records. {len(errors)} validation errors.")
    for error in errors:
        print(error)
    return int(bool(errors))


if __name__ == "__main__":
    raise SystemExit(main())

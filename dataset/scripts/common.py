"""Shared I/O, identity and approval functions for VoiceDoc data."""
import hashlib
import json
import os
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SFT = ROOT / "sft/candidates/sft_complete.jsonl"
RAG = ROOT / "rag/candidates/rag_complete.jsonl"
STYLES = {"english": "en", "telugu": "te", "translit": "te", "code": "te"}
CAPABILITIES = {
    "general": 2500, "symptom": 2000, "emergency": 1500,
    "grounded": 1500, "clarify": 1500, "multi": 1000,
}
LANGUAGE_TARGETS = {"english": 5000, "telugu": 3500, "translit": 750, "code": 750}
USAGE = {"extract_facts", "adapt", "translate", "machine_learning", "redistribute"}


def digest(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def read_rows(path):
    rows = []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        try:
            row = json.loads(line)
        except json.JSONDecodeError as error:
            raise ValueError(f"{path}:{number}: {error.msg}") from error
        if not isinstance(row, dict):
            raise ValueError(f"{path}:{number}: expected a JSON object")
        rows.append(row)
    return rows


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, encoding="utf-8", mode="w", delete=False) as tmp:
        pending = Path(tmp.name)
        try:
            tmp.write(data)
            tmp.close()
            os.replace(pending, path)
        finally:
            pending.unlink(missing_ok=True)


def write_json(path, data):
    write(path, json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n")


def write_rows(path, data):
    write(path, "".join(canonical(row) + "\n" for row in data))


def sources():
    return {row["source_id"]: row for row in read_json(ROOT / "sources/source_registry.json")}


def has_rights(source):
    return (
        source["rights_verified"] is True
        and source["license"]
        and source["license_evidence"]
        and USAGE.issubset(source["permitted_usage"])
    )


def approval_digest(row):
    visible = {key: value for key, value in row.items() if key not in {"clinician_reviewed", "reviewed_by"}}
    return digest(canonical(visible))


def require_approved(row, registry):
    key = row.get("id") or row["chunk_id"]
    receipt = row.get("approval")
    if (
        row.get("rights_verified") is not True
        or row.get("clinician_reviewed") is not True
        or not isinstance(row.get("reviewed_by"), str)
        or not row["reviewed_by"].strip()
        or not isinstance(receipt, dict)
        or receipt.get("reviewer") != row["reviewed_by"]
        or not isinstance(receipt.get("review_reference"), str)
        or not receipt["review_reference"].strip()
    ):
        raise ValueError(f"{key}: explicit rights, clinician and external review evidence are required")
    payload = {k: v for k, v in row.items() if k != "approval"}
    if receipt.get("record_sha256") != approval_digest(payload):
        raise ValueError(f"{key}: review receipt does not match the record")
    ids = row.get("source_ids", [row["source_id"]])
    if not ids or row["source_id"] not in ids or not all(
        source_id in registry and has_rights(registry[source_id]) for source_id in ids
    ):
        raise ValueError(f"{key}: one or more sources lack rights evidence")

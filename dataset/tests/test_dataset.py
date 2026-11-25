"""Candidate integrity, deterministic splitting, and unchanged native review gates."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "scripts"), str(ROOT.parent)]

from common import SFT, RAG, approval_digest, read_json, read_rows, require_approved, sources
from generate_dataset import materialize
from prepare_rag_export import build_sources
from split_dataset import split
from validate_dataset import validation
from ml.preparation.prepare_reviewed_sft import validate as validate_training_record
from ml.training.train_qlora import load_reviewed_jsonl
from rag.ingestion.build_pack import build, validate as validate_source


class CandidateDatasetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sft = read_rows(SFT)
        cls.rag = read_rows(RAG)
        cls.splits = {name: read_rows(ROOT / f"sft/splits/{name}.jsonl")
                      for name in ("train", "validation", "test")}
        cls.registry = sources()

    def test_deterministic_materialization_and_splits(self):
        self.assertEqual((self.sft, self.rag), materialize())
        self.assertEqual(self.splits, split(self.sft, 42))
        self.assertEqual([], validation(
            self.sft, self.rag, self.splits, self.registry,
            read_json(ROOT / "sources/license_manifest.json")))

    def test_existing_training_gate_rejects_every_candidate(self):
        for row in self.sft:
            with self.subTest(row=row["id"]), self.assertRaisesRegex(ValueError, "review"):
                validate_training_record(row)
        for name in self.splits:
            with self.assertRaisesRegex(ValueError, "review"):
                load_reviewed_jsonl(ROOT / f"sft/splits/{name}.jsonl")

    def test_native_rag_gate_rejects_every_candidate_and_writes_no_pack(self):
        exports = build_sources(self.rag)
        path = ROOT / "rag/source_export/candidate_sources.json"
        self.assertEqual(exports, json.loads(path.read_text(encoding="utf-8")))
        self.assertEqual(len({row["source_id"] for row in exports}), len(exports))
        for row in exports:
            with self.subTest(row=row["source_id"]), self.assertRaisesRegex(ValueError, "review"):
                validate_source(row)
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "knowledge.sqlite"
            with self.assertRaisesRegex(ValueError, "review"):
                build(path, output)
            self.assertFalse(output.exists())

    def test_review_receipts_bind_the_actual_payload(self):
        for row in [self.sft[0], self.rag[0]]:
            field = "response" if "response" in row else "text"
            original = approval_digest(row)
            self.assertNotEqual(original, approval_digest({**row, field: row[field] + " altered"}))
            with self.assertRaisesRegex(ValueError, "review"):
                require_approved(row, self.registry)

    def test_validator_detects_hash_tampering_and_split_leakage(self):
        changed = [{**self.rag[0], "text": self.rag[0]["text"] + " altered"}, *self.rag[1:]]
        leaked = {**self.splits, "test": self.splits["test"] + self.splits["train"][:1]}
        errors = validation(self.sft, changed, leaked, self.registry,
                            read_json(ROOT / "sources/license_manifest.json"))
        reasons = {error["reason"] for error in errors}
        self.assertIn("RAG text hash mismatch", reasons)
        self.assertIn("Topic or scenario leaked across splits", reasons)


if __name__ == "__main__":
    unittest.main()

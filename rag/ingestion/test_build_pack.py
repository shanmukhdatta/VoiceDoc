import hashlib
import json
import sqlite3
import tempfile
import unittest
from pathlib import Path
from build_pack import build


class BuildPackTests(unittest.TestCase):
    def source(self, *, approved=True):
        txt = "Synthetic research fixture only. Not a real clinical source."
        return {
            "source_id": "synthetic-only", "publisher": "Test fixture",
            "title": "Synthetic", "license": "test-permitted",
            "language": "en", "rights_verified": True,
            "clinician_reviewed": approved, "reviewed_by": "SIMULATED TEST FIXTURE",
            "text": txt, "content_sha256": hashlib.sha256(txt.encode()).hexdigest(),
        }

    def test_builds_sqlite_and_preserves_provenance(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            (base / "src.json").write_text(json.dumps([self.source()]), encoding="utf-8")
            report = build(base / "src.json", base / "pack.db")
            self.assertEqual(report["source_count"], 1)
            with sqlite3.connect(base / "pack.db") as connection:
                self.assertEqual(connection.execute("SELECT COUNT(*) FROM sources").fetchone()[0], 1)
                self.assertEqual(connection.execute("SELECT COUNT(*) FROM chunks").fetchone()[0], 1)
                self.assertEqual(connection.execute("SELECT source_id FROM sources").fetchone()[0], "synthetic-only")

    def test_unreviewed_corpus_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            (base / "src.json").write_text(json.dumps([self.source(approved=False)]))
            with self.assertRaises(ValueError):
                build(base / "src.json", base / "pack.db")
            self.assertFalse((base / "pack.db").exists())

    def test_tampering_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            example = self.source()
            example["text"] += " altered"
            (base / "src.json").write_text(json.dumps([example]))
            with self.assertRaises(ValueError):
                build(base / "src.json", base / "pack.db")


if __name__ == "__main__":
    unittest.main()

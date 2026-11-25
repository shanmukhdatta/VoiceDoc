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
            connection = sqlite3.connect(base / "pack.db")
            try:
                self.assertEqual(connection.execute("SELECT COUNT(*) FROM sources").fetchone()[0], 1)
                self.assertEqual(connection.execute("SELECT COUNT(*) FROM chunks").fetchone()[0], 1)
                self.assertEqual(connection.execute("SELECT source_id FROM sources").fetchone()[0], "synthetic-only")
            finally:
                connection.close()

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

    def test_empty_source_list_is_rejected_without_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            (base / "src.json").write_text("[]", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "empty knowledge pack"):
                build(base / "src.json", base / "pack.db")
            self.assertFalse((base / "pack.db").exists())

    def test_failed_staging_is_cleaned_and_existing_output_is_preserved(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            (base / "src.json").write_text(json.dumps([self.source()]), encoding="utf-8")
            output = base / "pack.db"
            output.write_bytes(b"existing")
            with self.assertRaises(FileExistsError):
                build(base / "src.json", output)
            self.assertEqual(output.read_bytes(), b"existing")
            self.assertEqual(list(base.glob(".pack.db.*.staging")), [])


if __name__ == "__main__":
    unittest.main()

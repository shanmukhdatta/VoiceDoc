import hashlib
import tempfile
import unittest
from pathlib import Path
from model_manifest import check_record
from prepare_reviewed_sft import split


class PreparationTests(unittest.TestCase):
    def test_manifest_correct_and_tampered(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            f = root / "model.bin"
            f.write_bytes(b"toy non-model test artifact")
            sample = {
                "id": "synthetic-test", "path": "model.bin",
                "sha256": hashlib.sha256(f.read_bytes()).hexdigest(), "bytes": f.stat().st_size,
                "format": "bin", "source": "test-fixture", "revision": "1",
                "license": "unit-test-only", "redistribution_reviewed": True, "languages": ["en", "te"]
            }
            self.assertEqual(check_record(sample, root)["status"],
                             "BYTE_INTEGRITY_CHECKED_NOT_CLINICALLY_VALIDATED")
            f.write_bytes(b"tamper")
            with self.assertRaises(ValueError):
                check_record(sample, root)
            sample["path"] = "../model.bin"
            with self.assertRaises(ValueError):
                check_record(sample, root)

    def test_reviewed_scenario_split_no_leakage(self):
        def row(n, language):
            return {"scenario_id": f"s{n}", "language": language, "prompt": "Synthetic",
                    "response": "Toy test", "source_id": "fixture", "license": "test",
                    "rights_verified": True, "clinician_reviewed": True,
                    "reviewed_by": "SIMULATED_FIXTURE"}
        train, validation = split([row(i, "en" if i % 2 else "te") for i in range(10)])
        self.assertTrue(train and validation)
        self.assertTrue({r["scenario_id"] for r in train}.isdisjoint(
            {r["scenario_id"] for r in validation}))
        bad = row(11, "te")
        bad["clinician_reviewed"] = False
        with self.assertRaises(ValueError):
            split([row(1, "en"), bad])


if __name__ == "__main__":
    unittest.main()

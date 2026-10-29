import hashlib
import tempfile
import unittest
from pathlib import Path
from model_manifest import check_pack, check_record
from prepare_reviewed_sft import split


class PreparationTests(unittest.TestCase):
    def asset(self, root, asset_id, role, path, fmt="onnx"):
        f = root / path
        f.write_bytes(("test fixture " + asset_id).encode())
        return {
            "id": asset_id, "role": role, "path": path,
            "sha256": hashlib.sha256(f.read_bytes()).hexdigest(), "bytes": f.stat().st_size,
            "format": fmt, "source": "synthetic-test", "revision": "fixture-1",
            "license": "unit-test-only", "redistribution_reviewed": True,
            "languages": ["en", "te"],
        }

    def pack(self, root):
        return {
            "schema_version": 1, "pack_id": "synthetic-e5", "version": "1.0.0",
            "kind": "embedding", "runtime": {"name": "onnxruntime-android", "version": "1.30.0"},
            "languages": ["en", "te"], "embedding_dimension": 384,
            "assets": [self.asset(root, "encoder", "model", "encoder.onnx"),
                       self.asset(root, "tokenizer", "tokenizer", "tokenizer.model", "bin")],
        }

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

    def test_versioned_pack_checks_required_assets_and_compatibility(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            result = check_pack(
                self.pack(root), root, expected_runtime="onnxruntime-android",
                expected_runtime_version="1.30.0", expected_embedding_dimension=384,
            )
            self.assertEqual(result["kind"], "embedding")
            self.assertEqual(len(result["assets"]), 2)
            self.assertIn("NOT_AUTHENTICATED", result["status"])

    def test_versioned_pack_rejects_runtime_dimension_missing_asset_and_tampering(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            pack = self.pack(root)
            with self.assertRaisesRegex(ValueError, "runtime version"):
                check_pack(pack, root, expected_runtime_version="0.0.0")
            with self.assertRaisesRegex(ValueError, "embedding dimension"):
                check_pack(pack, root, expected_embedding_dimension=512)
            pack["assets"].pop()
            with self.assertRaisesRegex(ValueError, "required embedding asset roles"):
                check_pack(pack, root)
            pack = self.pack(root)
            (root / "encoder.onnx").write_bytes(b"tampered")
            with self.assertRaisesRegex(ValueError, "size mismatch|digest mismatch"):
                check_pack(pack, root)

    def test_versioned_pack_rejects_duplicate_assets_and_empty_pack(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            pack = self.pack(root)
            pack["assets"].append(dict(pack["assets"][0]))
            with self.assertRaisesRegex(ValueError, "Duplicate asset"):
                check_pack(pack, root)
            pack["assets"] = []
            with self.assertRaisesRegex(ValueError, "at least one asset"):
                check_pack(pack, root)


if __name__ == "__main__":
    unittest.main()

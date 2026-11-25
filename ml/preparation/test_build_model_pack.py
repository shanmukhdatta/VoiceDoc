import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from build_model_pack import build_pack


def speech_pack(model: bytes):
    return {
        "schema_version": 1,
        "pack_id": "speech-fixture",
        "version": "1.0",
        "kind": "speech",
        "runtime": {"name": "whisper.cpp", "version": "1.9.5"},
        "languages": ["en", "te"],
        "assets": [{
            "id": "speech-model",
            "role": "model",
            "path": "assets/whisper.bin",
            "sha256": hashlib.sha256(model).hexdigest(),
            "bytes": len(model),
            "format": "ggml",
            "source": "synthetic test fixture",
            "revision": "fixture-1",
            "license": "test-only",
            "redistribution_reviewed": True,
            "languages": ["multi"],
        }],
    }


class BuildPackTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / "root"
        self.root.mkdir()
        self.asset = self.root / "assets" / "whisper.bin"
        self.asset.parent.mkdir()
        self.model = b"synthetic model bytes"
        self.asset.write_bytes(self.model)
        self.manifest = self.root / "manifest.json"
        self.data = json.dumps(speech_pack(self.model), ensure_ascii=False, indent=2).encode()
        self.manifest.write_bytes(self.data)
        self.output = Path(self.temp.name) / "speech.zip"

    def tearDown(self):
        self.temp.cleanup()

    def test_builds_exact_archive_and_reports_manifest_digest(self):
        result = build_pack(self.manifest, self.root, self.output, "whisper.cpp", "1.9.5")
        self.assertEqual(hashlib.sha256(self.data).hexdigest(), result["manifest_sha256"])
        self.assertTrue(result["integrity_only_not_authenticated"])
        with zipfile.ZipFile(self.output) as archive:
            self.assertEqual(["manifest.json", "assets/whisper.bin"], archive.namelist())
            self.assertEqual(self.data, archive.read("manifest.json"))
            self.assertEqual(self.model, archive.read("assets/whisper.bin"))

    def test_refuses_overwrite_and_preserves_existing_archive(self):
        self.output.write_bytes(b"keep this file")
        with self.assertRaises(FileExistsError):
            build_pack(self.manifest, self.root, self.output, "whisper.cpp", "1.9.5")
        self.assertEqual(b"keep this file", self.output.read_bytes())

    def test_bad_runtime_or_tampered_asset_never_publishes(self):
        with self.assertRaisesRegex(ValueError, "runtime"):
            build_pack(self.manifest, self.root, self.output, "other", "1.9.5")
        self.asset.write_bytes(b"tampered")
        with self.assertRaisesRegex(ValueError, "size mismatch|digest"):
            build_pack(self.manifest, self.root, self.output, "whisper.cpp", "1.9.5")
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()

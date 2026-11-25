import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from create_voicedoc_pack import create_voicedoc_pack


class CreateVoiceDocPackTests(unittest.TestCase):
    def test_creates_valid_voicedoc_bundle_zip_with_sidecar_digest(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            llm = root / "qwen3.litertlm"
            e5 = root / "encoder.onnx"
            tok = root / "tokenizer.json"
            db = root / "knowledge.sqlite"
            vad = root / "silero_vad.onnx"
            for p, content in (
                (llm, b"LITERTLM_BYTES"),
                (e5, b"ONNX_E5_BYTES"),
                (tok, b'{"model":{"vocab":[]}}'),
                (db, b"SQLite format 3\x00" + b"0" * 100),
                (vad, b"ONNX_VAD_BYTES"),
            ):
                p.write_bytes(content)

            out_zip = root / "VoiceDoc_Model_Pack.zip"
            report = create_voicedoc_pack(out_zip, llm, e5, tok, db, vad_model=vad)
            self.assertTrue(out_zip.is_file())
            self.assertEqual(report["asset_count"], 5)
            self.assertEqual(len(report["manifest_sha256"]), 64)
            self.assertTrue(Path(report["manifest_sha256_file"]).is_file())

            with zipfile.ZipFile(out_zip, "r") as zf:
                names = set(zf.namelist())
                self.assertIn("manifest.json", names)
                manifest = json.loads(zf.read("manifest.json").decode("utf-8"))
                self.assertEqual(manifest["kind"], "bundle")
                self.assertEqual(manifest["embedding_dimension"], 384)


if __name__ == "__main__":
    unittest.main()

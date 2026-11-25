import json
import tempfile
import unittest
from pathlib import Path

from convert_qwen3_litertlm import validate_merged_checkpoint, write_conversion_metadata


def write_fake_qwen3_checkpoint(root: Path, model_type: str = "qwen3") -> None:
    root.mkdir(parents=True, exist_ok=True)
    (root / "config.json").write_text(json.dumps({
        "model_type": model_type,
        "hidden_size": 1024,
        "num_hidden_layers": 28,
        "max_position_embeddings": 32768,
    }), encoding="utf-8")
    (root / "model.safetensors").write_bytes(b"SAFETENSORS_TEST_STUB")
    (root / "tokenizer.json").write_text("{}", encoding="utf-8")
    (root / "tokenizer_config.json").write_text(json.dumps({"chat_template": "{% for m in messages %}{% endfor %}"}), encoding="utf-8")


class ConvertQwen3Tests(unittest.TestCase):
    def test_validates_qwen3_checkpoint_structure(self):
        with tempfile.TemporaryDirectory() as tmp:
            ckpt = Path(tmp) / "merged"
            write_fake_qwen3_checkpoint(ckpt)
            info = validate_merged_checkpoint(ckpt, max_context_len=2048)
            self.assertEqual(info["model_type"], "qwen3")
            self.assertEqual(info["safetensors_files"], ["model.safetensors"])

    def test_rejects_non_qwen3_architecture(self):
        with tempfile.TemporaryDirectory() as tmp:
            ckpt = Path(tmp) / "merged"
            write_fake_qwen3_checkpoint(ckpt, model_type="llama")
            with self.assertRaisesRegex(ValueError, "qwen3"):
                validate_merged_checkpoint(ckpt)

    def test_rejects_excessive_context_length(self):
        with tempfile.TemporaryDirectory() as tmp:
            ckpt = Path(tmp) / "merged"
            write_fake_qwen3_checkpoint(ckpt)
            with self.assertRaisesRegex(ValueError, "exceeds"):
                validate_merged_checkpoint(ckpt, max_context_len=65536)

    def test_writes_conversion_metadata_with_sha256(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out"
            out.mkdir()
            artifact = out / "qwen3-0.6b.litertlm"
            artifact.write_bytes(b"LITERTLM_TEST")
            meta = write_conversion_metadata(out, artifact, "litertlm", "int4", 2048, {"model_type": "qwen3"})
            data = json.loads(meta.read_text(encoding="utf-8"))
            self.assertEqual(data["target_runtime"], "litertlm")
            self.assertEqual(data["target_runtime_version"], "0.18.0")
            self.assertEqual(data["quantization_recipe"], "dynamic_wi4b32_afp32")
            self.assertEqual(len(data["artifact_sha256"]), 64)


if __name__ == "__main__":
    unittest.main()

import json
import tempfile
import unittest
from pathlib import Path

from train_qlora import SYSTEM_PROMPT, check_splits, load_reviewed_jsonl, tokenize_example


def reviewed(scenario_id="scenario-1", language="en"):
    return {
        "scenario_id": scenario_id,
        "language": language,
        "prompt": "Context: reviewed source passage. Question: what does it say?",
        "response": "The passage says to consult a clinician when concerned.",
        "source_id": "synthetic-source",
        "license": "test-only",
        "rights_verified": True,
        "clinician_reviewed": True,
        "reviewed_by": "synthetic-test-reviewer",
    }


class FakeTokenizer:
    def apply_chat_template(self, messages, tokenize, add_generation_prompt, enable_thinking=False):
        assert tokenize is True
        text = "|".join(f"{item['role']}:{item['content']}" for item in messages)
        if add_generation_prompt:
            text += "|assistant:"
        return [ord(character) for character in text]


class QLoRATests(unittest.TestCase):
    def test_loads_reviewed_rows_from_jsonl(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "rows.jsonl"
            path.write_text(json.dumps(reviewed(), ensure_ascii=False) + "\n", encoding="utf-8")
            self.assertEqual(1, len(load_reviewed_jsonl(path)))

    def test_rejects_unreviewed_data(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "rows.jsonl"
            row = reviewed()
            row["rights_verified"] = False
            path.write_text(json.dumps(row) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Rights and clinician review"):
                load_reviewed_jsonl(path)

    def test_group_split_leakage_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Scenario leakage"):
            check_splits([reviewed("shared")], [reviewed("shared")])

    def test_assistant_loss_masks_system_and_user_prompt(self):
        item = tokenize_example(FakeTokenizer(), reviewed(), max_length=2048)
        prompt = FakeTokenizer().apply_chat_template(
            [{"role": "system", "content": SYSTEM_PROMPT}, {"role": "user", "content": reviewed()["prompt"]}],
            tokenize=True, add_generation_prompt=True
        )
        self.assertTrue(all(label == -100 for label in item["labels"][:len(prompt)]))
        self.assertTrue(all(label != -100 for label in item["labels"][len(prompt):]))

    def test_overlong_answer_is_not_silently_truncated(self):
        with self.assertRaisesRegex(ValueError, "rather than truncating"):
            tokenize_example(FakeTokenizer(), reviewed(), max_length=128)

    def test_qwen3_non_thinking_prefix_is_masked_from_loss(self):
        class Qwen3NonThinkingTokenizer:
            def apply_chat_template(self, messages, tokenize, add_generation_prompt, enable_thinking=False):
                assert tokenize is True
                assert enable_thinking is False
                parts = [f"<|im_start|>{m['role']}\n{m['content']}<|im_end|>\n" for m in messages]
                if add_generation_prompt:
                    parts.append("<|im_start|>assistant\n<think>\n\n</think>\n\n")
                else:
                    # Replace last assistant turn to include empty think block
                    last = messages[-1]
                    assert last["role"] == "assistant"
                    parts[-1] = f"<|im_start|>assistant\n<think>\n\n</think>\n\n{last['content']}<|im_end|>\n"
                return [ord(c) for c in "".join(parts)]

        tok = Qwen3NonThinkingTokenizer()
        row = reviewed(language="te")
        row["prompt"] = "ఆరోగ్య సమాచారం ఏమిటి?"
        row["response"] = "వైద్యుని సంప్రదించండి."
        item = tokenize_example(tok, row, max_length=2048)
        prompt_ids = tok.apply_chat_template(
            [{"role": "system", "content": SYSTEM_PROMPT}, {"role": "user", "content": row["prompt"]}],
            tokenize=True, add_generation_prompt=True, enable_thinking=False,
        )
        self.assertEqual(item["input_ids"][:len(prompt_ids)], prompt_ids)
        self.assertTrue(all(lbl == -100 for lbl in item["labels"][:len(prompt_ids)]))
        self.assertTrue(all(lbl != -100 for lbl in item["labels"][len(prompt_ids):]))

    def test_training_config_matches_qwen3_06b(self):
        cfg = json.loads((Path(__file__).resolve().parent / "config.json").read_text(encoding="utf-8"))
        self.assertEqual(cfg["model_id"], "Qwen/Qwen3-0.6B")
        self.assertEqual(cfg["max_seq_len"], 2048)
        self.assertGreaterEqual(cfg["lora_rank"], 8)


if __name__ == "__main__":
    unittest.main()

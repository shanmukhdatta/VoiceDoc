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
    def apply_chat_template(self, messages, tokenize, add_generation_prompt):
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


if __name__ == "__main__":
    unittest.main()

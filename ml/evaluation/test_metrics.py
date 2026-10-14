import unittest
from metrics import edit_distance, word_error_rate, character_error_rate, retrieval_recall, urgency_recall, evaluate

class MetricsTests(unittest.TestCase):
    def test_edit_distance(self):
        self.assertEqual(edit_distance("abc", "adc"), 1)
    def test_word_error_rate(self):
        self.assertEqual(word_error_rate([("hello world", "hello")]), 0.5)
    def test_character_error_rate(self):
        self.assertEqual(character_error_rate([("te", "ta")]), 0.5)
    def test_retrieval_recall(self):
        r=[{"relevant_ids":["a"],"retrieved_ids":["x","a"]}]
        self.assertEqual(retrieval_recall(r, 1), 0)
        self.assertEqual(retrieval_recall(r, 3), 1)
    def test_urgency(self):
        self.assertEqual(urgency_recall([
            {"urgent_gold":True,"escalated":True},
            {"urgent_gold":True,"escalated":False}]), 0.5)
    def test_bilingual(self):
        r=evaluate([{"language":"te","task":"asr","reference":"తెలుగు","prediction":"తెలుగు"}])
        self.assertEqual(r["te"]["cer"], 0)
    def test_invalid_language(self):
        with self.assertRaises(ValueError):
            evaluate([{"language":"ta","task":"asr","reference":"a","prediction":"a"}])

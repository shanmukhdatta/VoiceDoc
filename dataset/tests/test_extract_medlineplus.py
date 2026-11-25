import hashlib
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from extract_medlineplus import extract


class ExtractMedlinePlusTest(unittest.TestCase):
    def test_reads_only_english_summary_and_preserves_provenance(self):
        xml = b"""<health-topics>
          <health-topic language="English" title="Example" url="https://medlineplus.gov/example.html">
            <full-summary><p>First line.</p><p>Follow <a href="/example2.html">the link</a>.</p></full-summary>
            <site><standard-description>External page material.</standard-description></site>
            <group>Example Group</group>
          </health-topic>
          <health-topic language="Spanish" title="Ejemplo" url="https://medlineplus.gov/example2.html">
            <full-summary>Exclude this other language.</full-summary>
          </health-topic>
        </health-topics>"""
        items = extract(xml, "2026-11-25")
        self.assertEqual(1, len(items))
        row = items[0]
        self.assertEqual("nlm-example", row["source_id"])
        self.assertEqual("First line. Follow the link .", row["summary_text"])
        self.assertEqual(hashlib.sha256(xml).hexdigest(), row["snapshot_sha256"])
        self.assertIsNone(row["publication_date"])

    def test_rejects_executably_expanded_xml_entities(self):
        xml = b'<!DOCTYPE health-topics [<!ENTITY phrase "avoid">]><health-topics/>'
        with self.assertRaisesRegex(ValueError, "Entity declarations"):
            extract(xml, "2026-11-25")

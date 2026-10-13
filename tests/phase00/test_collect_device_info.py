"""Pure-stdlib smoke tests; these DO NOT test actual Android hardware."""
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts" / "phase00"))
import collect_device_info as collector  # noqa: E402


class DeviceInfoTests(unittest.TestCase):
    def test_meminfo_parser(self):
        self.assertEqual(collector.parse_memtotal_kib("MemTotal:       8192000 kB\n"), 8192000)
        self.assertIsNone(collector.parse_memtotal_kib("MemFree: 111 kB\n"))
        self.assertIsNone(collector.parse_memtotal_kib("MemTotal: unknown kB\n"))

    def test_missing_adb_returns_code_2(self):
        with patch.object(collector.shutil, "which", return_value=None):
            with patch.object(sys, "argv", ["collector"]):
                self.assertEqual(collector.main(), 2)

    def test_mocked_collection_marks_every_model_not_run(self):
        def fake_adb(*args):
            if args == ("get-state",):
                return "device"
            if args[-2:] == ("cat", "/proc/meminfo"):
                return "MemTotal: 8192000 kB\n"
            if args[-3:] == ("df", "-k", "/data"):
                return "Filesystem 1K-blocks Used Available Use% Mounted on\n/data 64000000 10000000 54000000 16% /data"
            if args[:2] == ("shell", "getprop"):
                return "test-value"
            raise AssertionError(f"Unexpected fake adb call: {args}")

        with tempfile.TemporaryDirectory() as directory:
            output = str(Path(directory) / "inventory.json")
            with patch.object(collector.shutil, "which", return_value="/fake/adb"):
                with patch.object(collector, "adb", side_effect=fake_adb):
                    with patch.object(sys, "argv", ["collector", "--output", output]):
                        self.assertEqual(collector.main(), 0)
            data = json.loads(Path(output).read_text(encoding="utf-8"))
            self.assertEqual(data["physical_ram_gib_approx"], round(8192000 / (1024 ** 2), 2))
            self.assertTrue(all(v == "NOT_RUN" for v in data["assessments"].values()))
            self.assertNotIn("serial", json.dumps(data).lower())


if __name__ == "__main__":
    unittest.main()

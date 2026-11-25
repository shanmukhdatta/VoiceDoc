"""No SDK or model downloads required for these synthetic binary fixtures."""
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path
from audit_native_16k import audit, elf_issues


def elf64(alignment: int) -> bytes:
    blob = bytearray(120)
    blob[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<Q", blob, 32, 64)  # e_phoff
    struct.pack_into("<HH", blob, 54, 56, 1)  # e_phentsize, e_phnum
    struct.pack_into("<I", blob, 64, 1)  # PT_LOAD
    struct.pack_into("<Q", blob, 64 + 48, alignment)
    return bytes(blob)


class NativeAlignmentTests(unittest.TestCase):
    def test_elf64_good_and_bad_load_alignment(self):
        self.assertEqual([], elf_issues(elf64(16384)))
        self.assertTrue(any("4096" in issue for issue in elf_issues(elf64(4096))))
        self.assertTrue(elf_issues(b"invalid"))

    def test_apk_audit_elf_independently_of_zip_compression(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.apk"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("lib/arm64-v8a/libgood.so", elf64(16384), compress_type=zipfile.ZIP_DEFLATED)
                archive.writestr("lib/arm64-v8a/libbad.so", elf64(4096), compress_type=zipfile.ZIP_DEFLATED)
            count, issues = audit(path)
            self.assertEqual(2, count)
            self.assertEqual(1, len(issues))
            self.assertIn("libbad.so", issues[0])

    def test_uncompressed_library_must_have_zip_alignment(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.apk"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("lib/arm64-v8a/libgood.so", elf64(16384), compress_type=zipfile.ZIP_STORED)
            _, issues = audit(path)
            self.assertTrue(any("stored ZIP offset" in issue for issue in issues))

    def test_empty_apk_has_no_native_warnings(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.apk"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("assets/README.txt", "fixture")
            self.assertEqual((0, []), audit(path))


if __name__ == "__main__":
    unittest.main()

#!/usr/bin/env python3
"""Audit native ELF LOAD alignment and uncompressed APK ZIP alignment (16 KiB).

This is a static compatibility gate, NOT a replacement for running on a
16 KiB-page Android device. Uses the Android Developer page-size guidance.
"""
from __future__ import annotations

import argparse
import struct
import sys
import zipfile
from pathlib import Path

PAGE = 16 * 1024


def elf_issues(blob: bytes) -> list[str]:
    if len(blob) < 64 or blob[:4] != b"\x7fELF":
        return ["not a valid ELF header"]
    cls, encoding = blob[4], blob[5]
    if cls not in (1, 2) or encoding not in (1, 2):
        return ["unsupported ELF class or endianness"]
    order = "<" if encoding == 1 else ">"
    if cls == 2:  # ELF64
        off = struct.unpack_from(order + "Q", blob, 32)[0]
        size, count = struct.unpack_from(order + "HH", blob, 54)
        minimum_size, align_offset, align_type = 56, 48, "Q"
    else:  # ELF32
        off = struct.unpack_from(order + "I", blob, 28)[0]
        size, count = struct.unpack_from(order + "HH", blob, 42)
        minimum_size, align_offset, align_type = 32, 28, "I"
    if count == 0 or size < minimum_size or off > len(blob) or count > (len(blob) - off) // size:
        return ["missing or truncated ELF program headers"]
    problems = []
    loads = 0
    for index in range(count):
        start = off + index * size
        kind = struct.unpack_from(order + "I", blob, start)[0]
        if kind != 1:  # PT_LOAD
            continue
        loads += 1
        align = struct.unpack_from(order + align_type, blob, start + align_offset)[0]
        if align < PAGE or align % PAGE != 0:
            problems.append(f"PT_LOAD[{index}] p_align={align} (requires 16384-byte multiple)")
    if not loads:
        problems.append("no PT_LOAD segments")
    return problems


def stored_data_offset(handle, entry: zipfile.ZipInfo) -> int:
    handle.seek(entry.header_offset)
    header = handle.read(30)
    if len(header) != 30 or header[:4] != b"PK\x03\x04":
        raise ValueError("invalid local ZIP header")
    filename_length, extra_length = struct.unpack_from("<HH", header, 26)
    return entry.header_offset + 30 + filename_length + extra_length


def audit(apk: Path) -> tuple[int, list[str]]:
    problems: list[str] = []
    native_count = 0
    with zipfile.ZipFile(apk) as archive, apk.open("rb") as raw:
        for entry in archive.infolist():
            if not entry.filename.startswith("lib/") or not entry.filename.endswith(".so"):
                continue
            native_count += 1
            try:
                for issue in elf_issues(archive.read(entry)):
                    problems.append(f"{entry.filename}: {issue}")
                if entry.compress_type == zipfile.ZIP_STORED:
                    offset = stored_data_offset(raw, entry)
                    if offset % PAGE:
                        problems.append(f"{entry.filename}: stored ZIP offset {offset} is not 16 KiB aligned")
            except (OSError, ValueError, struct.error, RuntimeError, zipfile.BadZipFile) as error:
                problems.append(f"{entry.filename}: inspection error ({type(error).__name__})")
    return native_count, problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, help="existing Android APK to inspect")
    args = parser.parse_args()
    try:
        count, problems = audit(args.apk)
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"AUDIT ERROR: {error}", file=sys.stderr)
        return 2
    print(f"Checked {count} native .so libraries in {args.apk}")
    for problem in problems:
        print("FAIL:", problem)
    if problems:
        print("FAIL: native packaging is not confirmed 16 KiB compatible")
        return 1
    print("PASS: static native alignment audit (16 KiB device test still required)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

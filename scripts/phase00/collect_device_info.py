#!/usr/bin/env python3
"""Phase 00: collect non-identifying Android hardware facts via adb.

Requires Python 3.10+ and Android platform-tools (adb) on PATH.
Does NOT access recordings, health information, contacts, or device serial IDs.
Does NOT download or test model assets. Review output before sharing.
"""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

PROPERTIES = {
    "manufacturer": "ro.product.manufacturer",
    "model": "ro.product.model",
    "android_release": "ro.build.version.release",
    "android_api": "ro.build.version.sdk",
    "supported_abis": "ro.product.cpu.abilist",
    "soc_model": "ro.soc.model",
    "hardware": "ro.hardware",
}


def adb(*args: str) -> str:
    process = subprocess.run(
        ["adb", *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
        timeout=20,
    )
    if process.returncode:
        raise RuntimeError(
            f"adb command failed ({process.returncode}); confirm USB debugging, "
            f"authorization and platform-tools are working. stderr: {process.stderr.strip()[:250]}"
        )
    return process.stdout.strip()


def parse_memtotal_kib(meminfo: str) -> int | None:
    for line in meminfo.splitlines():
        if line.startswith("MemTotal:"):
            tokens = line.split()
            if len(tokens) >= 2 and tokens[1].isdigit():
                return int(tokens[1])
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default="device-report.json", help="JSON output (avoid committing to Git)")
    args = parser.parse_args()
    if not shutil.which("adb"):
        print("adb was not found on PATH. Install Android SDK platform-tools.", file=sys.stderr)
        return 2
    try:
        state = adb("get-state")
        if state != "device":
            raise RuntimeError("Expected exactly one connected and authorized adb device.")
        properties = {key: adb("shell", "getprop", prop) for key, prop in PROPERTIES.items()}
        meminfo = adb("shell", "cat", "/proc/meminfo")
        storage = adb("shell", "df", "-k", "/data")
    except (RuntimeError, subprocess.TimeoutExpired) as exc:
        print(f"Collection failed: {exc}", file=sys.stderr)
        return 1
    ram_kib = parse_memtotal_kib(meminfo)
    report = {
        "status": "HARDWARE_INFO_ONLY_NOT_A_MODEL_TEST",
        "collected_at_utc": datetime.now(timezone.utc).isoformat(),
        "device": properties,
        "physical_ram_gib_approx": round(ram_kib / (1024 ** 2), 2) if ram_kib else None,
        "storage_df_kib_raw": storage,
        "assessments": {
            "llm_load": "NOT_RUN",
            "asr": "NOT_RUN",
            "embedding": "NOT_RUN",
            "english_tts": "NOT_RUN",
            "telugu_tts": "NOT_RUN",
            "offline_end_to_end": "NOT_RUN",
        },
    }
    destination = Path(args.output)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote non-identifying device inventory to {destination}")
    print("Review the output for any unexpected device strings before sharing.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

"""Compare actual greedy token IDs from separately executed reference runs."""
import argparse
import json
from pathlib import Path

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("resident", type=Path)
    p.add_argument("candidates", type=Path, nargs="+")
    a = p.parse_args()
    baseline = json.loads(a.resident.read_text())
    if baseline["mode"] != "resident":
        p.error("First result must be resident")
    passed = True
    for path in a.candidates:
        row = json.loads(path.read_text())
        keys = ("prompt", "dtype", "max_tokens", "threads")
        if any(row[k] != baseline[k] for k in keys):
            p.error(f"Incomparable configuration: {path}")
        ok = row["token_ids"] == baseline["token_ids"]
        print(f"{path}: greedy token parity={ok}")
        passed &= ok
    return 0 if passed else 1

if __name__ == "__main__":
    raise SystemExit(main())

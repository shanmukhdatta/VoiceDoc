"""Export explicitly reviewed records while preserving the existing three candidate splits."""
import argparse
from pathlib import Path
import sys

from common import ROOT, SFT, approval_digest, read_rows, require_approved, sources, write_rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reviewed", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    sys.path.insert(0, str(ROOT.parent))
    from ml.preparation.prepare_reviewed_sft import validate
    from ml.training.train_qlora import check_splits

    registry = sources()
    baseline = {row["id"]: row for row in read_rows(SFT)}
    approved = {}
    for row in read_rows(args.reviewed):
        key = row["id"]
        require_approved(row, registry)
        if key in approved or key not in baseline or row["approval"]["record_sha256"] != approval_digest(baseline[key]):
            raise ValueError(f"{key}: duplicate, unknown, or changed candidate")
        validate(row)
        approved[key] = {k: v for k, v in row.items() if k != "approval"}

    splits = {name: [approved[row["id"]] for row in read_rows(ROOT / f"sft/splits/{name}.jsonl")
                     if row["id"] in approved] for name in ("train", "validation", "test")}
    if not all(splits.values()) or sum(map(len, splits.values())) != len(approved):
        raise ValueError("Approved records must be assigned once each with all three splits non-empty")
    check_splits(splits["train"], splits["validation"])
    check_splits(splits["train"] + splits["validation"], splits["test"])
    for name, rows in splits.items():
        write_rows(args.output_dir / f"{name}.jsonl", rows)
        print(f"{name}: {len(rows)} approved records")


if __name__ == "__main__":
    main()

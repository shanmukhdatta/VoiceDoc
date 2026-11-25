"""Independent idealized two-stage schedule; no inference or device measurements."""
import argparse
import csv
import json
import math

def evaluate(rows):
    io_end = compute_end = serial = 0.0
    timeline = []
    seen = set()
    for row in rows:
        step = row["step"]
        load, compute = float(row["load_ms"]), float(row["compute_ms"])
        if step in seen or not step or not all(
                math.isfinite(v) and v >= 0 for v in (load, compute)):
            raise ValueError("Invalid or duplicate step")
        seen.add(step)
        load_start = io_end
        io_end += load
        compute_start = max(io_end, compute_end)
        compute_end = compute_start + compute
        serial += load + compute
        timeline.append(dict(step=step, load_start_ms=load_start,
                             load_end_ms=io_end, compute_start_ms=compute_start,
                             compute_end_ms=compute_end))
    if not timeline:
        raise ValueError("Trace must contain at least one step")
    return dict(kind="analytical_not_inference", serial_ms=serial,
                ideal_overlap_ms=compute_end,
                ideal_speedup=serial/compute_end if compute_end else None,
                timeline=timeline)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("trace")
    args = p.parse_args()
    with open(args.trace, newline="", encoding="utf-8") as f:
        print(json.dumps(evaluate(csv.DictReader(f)), indent=2, allow_nan=False))

if __name__ == "__main__":
    main()

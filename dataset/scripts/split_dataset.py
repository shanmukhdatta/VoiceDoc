"""Assign entire subject groups to seeded, approximately stratified splits."""
import argparse
from collections import Counter, defaultdict
import random

from common import ROOT, SFT, read_rows, write_rows

RATIOS = {"train": .8, "validation": .1, "test": .1}


def profile(rows):
    counts = Counter({"total": len(rows)})
    for row in rows:
        counts["style:" + row["language_style"]] += 1
        counts["capability:" + row["capability"]] += 1
    return counts


def split(rows, seed=42):
    groups = defaultdict(list)
    scenario_groups = {}
    for row in rows:
        key = row["split_group"]
        if scenario_groups.setdefault(row["scenario_id"], key) != key:
            raise ValueError("One scenario cannot appear in multiple split groups")
        groups[key].append(row)
    if len(groups) < len(RATIOS):
        raise ValueError("Need at least three independent groups")
    randomizer = random.Random(seed)
    order = sorted(groups)
    randomizer.shuffle(order)
    order.sort(key=lambda key: -len(groups[key]))
    totals = profile(rows)
    assigned = {name: [] for name in RATIOS}
    current = {name: Counter() for name in RATIOS}
    emergency = [key for key in order if any(row["capability"] == "emergency" for row in groups[key])]
    held_out_capacity = round(len(rows) * RATIOS["test"])
    eligible = [key for key in emergency if len(groups[key]) <= held_out_capacity]
    if len(emergency) >= 3 and len(eligible) >= 2:
        eligible.sort(key=lambda key: (len(groups[key]), profile(groups[key])["capability:emergency"]))
        anchors = {"validation": eligible[0], "test": eligible[1]}
        anchors["train"] = next(key for key in emergency if key not in anchors.values())
        for name, key in anchors.items():
            assigned[name].extend(groups[key])
            current[name].update(profile(groups[key]))
        order = [key for key in order if key not in anchors.values()]
    for i, key in enumerate(order):
        batch = profile(groups[key])
        available = [name for name, values in assigned.items() if not values]
        options = available if len(available) == len(order) - i else list(RATIOS)

        def increase(name):
            score = 0
            for dimension, count in batch.items():
                target = totals[dimension] * RATIOS[name]
                before = current[name][dimension] - target
                weight = 4 if dimension == "total" else 1
                score += weight * ((before + count) ** 2 - before ** 2) / max(target, 1)
            return score

        target = min(options, key=increase)
        assigned[target].extend(groups[key])
        current[target].update(batch)
    return {name: sorted(values, key=lambda row: row["id"]) for name, values in assigned.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    for name, rows in split(read_rows(SFT), args.seed).items():
        write_rows(ROOT / f"sft/splits/{name}.jsonl", rows)
        print(f"{name}: {len(rows)}")


if __name__ == "__main__":
    main()

"""Plan or explicitly execute a non-medical ALLM CLI experiment."""
import argparse
import json
import pathlib
import re
import shlex
import subprocess
import time

PROMPTS = ("The capital of France is", "భారతదేశ రాజధాని")
BUDGETS = (None, "64MB", "128MB", "256MB")

def command(args, *parts):
    items = [args.binary, *parts]
    if args.adb:
        prefix = ["adb"] + (["-s", args.serial] if args.serial else [])
        return prefix + ["shell", shlex.join(items)]
    return items

def invoke(cmd, timeout):
    start = time.monotonic()
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8",
                           errors="replace", timeout=timeout, check=False)
        return dict(command=cmd, exit_code=p.returncode, stdout=p.stdout,
                    stderr=p.stderr, wall_seconds=time.monotonic()-start)
    except (OSError, subprocess.TimeoutExpired) as exc:
        return dict(command=cmd, exit_code=None, error=str(exc),
                    wall_seconds=time.monotonic()-start)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--binary", required=True)
    p.add_argument("--model", required=True)
    p.add_argument("--adb", action="store_true")
    p.add_argument("--serial")
    p.add_argument("--threads", type=int, default=4)
    p.add_argument("--max-tokens", type=int, default=24)
    p.add_argument("--timeout", type=int, default=300)
    p.add_argument("--output", type=pathlib.Path, required=True)
    p.add_argument("--execute", action="store_true")
    a = p.parse_args()
    if not 1 <= a.threads <= 16 or not 1 <= a.max_tokens <= 64 or a.timeout <= 0:
        p.error("Invalid threads, token limit or timeout")
    if a.serial and not a.adb:
        p.error("--serial requires --adb")
    inspect = command(a, "inspect", a.model)
    runs = []
    for prompt in PROMPTS:
        for budget in BUDGETS:
            flags = ["run", a.model, "-p", prompt, "--max-tokens", str(a.max_tokens),
                     "--threads", str(a.threads), "--temperature", "0", "--seed", "0"]
            if budget:
                flags += ["--mem-budget", budget]
            runs.append(dict(prompt=prompt, budget=budget or "resident",
                             command=command(a, *flags)))
    report = dict(status="planned", target="Qwen2.5-0.5B control only",
                  inspection_command=inspect, runs=runs)
    if a.execute:
        result = invoke(inspect, a.timeout)
        report["inspection"] = result
        # Fail closed: only permit the architecture supported by the control.
        # Architecture metadata is not proof of exact checkpoint provenance.
        meta = result.get("stdout", "")
        if result["exit_code"] != 0 or not re.search(
                r"(?im)^\s*(?:general\.)?architecture\s*[:=]\s*qwen2\s*$", meta):
            report["status"] = "blocked_architecture_or_inspection"
        else:
            for run in runs:
                run["result"] = invoke(run["command"], a.timeout)
            for prompt in PROMPTS:
                same = [r for r in runs if r["prompt"] == prompt]
                ref = same[0]["result"]
                for row in same:
                    current = row["result"]
                    row["text_parity_vs_resident"] = (
                        current["stdout"] == ref["stdout"]
                        if current["exit_code"] == ref["exit_code"] == 0 else None)
            report["status"] = ("executed_unvalidated" if all(
                r["result"]["exit_code"] == 0 for r in runs) else "execution_failed")
    a.output.parent.mkdir(parents=True, exist_ok=True)
    with a.output.open("x", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(report["status"])
    return 0 if report["status"] in ("planned", "executed_unvalidated") else 1

if __name__ == "__main__":
    raise SystemExit(main())

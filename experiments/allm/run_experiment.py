"""Plan or run resident/streamed Qwen3 GGUF experiments on host or adb."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess
import time

PROMPTS = ("The capital of France is", "భారతదేశ రాజధాని")
BUDGETS = (None, "64MB", "128MB", "256MB")

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

def ids(stderr, key):
    m = re.search(r"(?m)^" + key + r"=(\[[0-9, ]*\])\s*$", stderr)
    return json.loads(m.group(1)) if m else None

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--binary", required=True)
    p.add_argument("--model", required=True)
    p.add_argument("--adb", action="store_true")
    p.add_argument("--serial")
    p.add_argument("--threads", type=int, default=4)
    p.add_argument("--max-tokens", type=int, default=16)
    p.add_argument("--timeout", type=int, default=300)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--execute", action="store_true")
    a = p.parse_args()
    if not 1 <= a.threads <= 16 or not 1 <= a.max_tokens <= 64 or a.timeout <= 0:
        p.error("Invalid threads, token limit or timeout")
    if a.serial and not a.adb:
        p.error("--serial requires --adb")
    prefix = ["adb"] + (["-s", a.serial] if a.serial else []) + ["shell"]
    def command(*parts):
        cmd = [a.binary, *parts]
        return prefix + [shlex.join(cmd)] if a.adb else cmd
    report = dict(status="planned", target="dense Qwen3 port (unvalidated)",
                  model=a.model, max_tokens=a.max_tokens, threads=a.threads, runs=[])
    for prompt in PROMPTS:
        for budget in BUDGETS:
            flags = ["run", a.model, "-p", prompt, "--max-tokens", str(a.max_tokens),
                     "--threads", str(a.threads), "--temperature", "0", "--seed", "0"]
            if budget:
                flags += ["--mem-budget", budget]
            report["runs"].append(dict(prompt=prompt, budget=budget or "resident",
                                       command=command(*flags)))
    if a.execute:
        # Hash the exact model used, including on the Android device.
        if a.adb:
            h = invoke(prefix + [shlex.join(["sha256sum", a.model])], a.timeout)
            digest = h.get("stdout", "").split()
            if h["exit_code"] != 0 or not digest or not re.fullmatch(r"[0-9a-f]{64}", digest[0]):
                p.error("Could not hash device model")
            report["model_sha256"] = digest[0]
        else:
            h = hashlib.sha256()
            with open(a.model, "rb") as f:
                for chunk in iter(lambda: f.read(1024*1024), b""):
                    h.update(chunk)
            report["model_sha256"] = h.hexdigest()
        check = invoke(command("inspect", a.model), a.timeout)
        report["inspection"] = check
        if check["exit_code"] != 0 or not re.search(
                r"(?im)^Architecture:\s*qwen3\s*$", check.get("stdout", "")):
            report["status"] = "blocked_not_dense_qwen3"
        else:
            for run in report["runs"]:
                run["result"] = invoke(run["command"], a.timeout)
                stderr = run["result"].get("stderr", "")
                run["prompt_ids"] = ids(stderr, "VOICEDOC_PROMPT_IDS")
                run["generated_ids"] = ids(stderr, "VOICEDOC_GENERATED_IDS")
            valid = all(r["result"]["exit_code"] == 0 and r["prompt_ids"]
                        and r["generated_ids"] is not None for r in report["runs"])
            for prompt in PROMPTS:
                rows = [r for r in report["runs"] if r["prompt"] == prompt]
                for row in rows:
                    row["parity_vs_resident"] = (
                        row["prompt_ids"] == rows[0]["prompt_ids"] and
                        row["generated_ids"] == rows[0]["generated_ids"]) if valid else None
            report["status"] = ("executed_unvalidated" if valid else "execution_failed")
    a.output.parent.mkdir(parents=True, exist_ok=True)
    with a.output.open("x", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(report["status"])
    return 0 if report["status"] in ("planned", "executed_unvalidated") else 1

if __name__ == "__main__":
    raise SystemExit(main())

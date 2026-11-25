"""Compare patched ALLM greedy output against llama.cpp for the exact same GGUF."""
import argparse
import hashlib
import json
from pathlib import Path

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--report", type=Path, required=True)
    p.add_argument("--gguf", type=Path, required=True)
    a = p.parse_args()
    report = json.loads(a.report.read_text())
    if report["status"] != "executed_unvalidated":
        p.error("Expected an executed report with patched CLI token IDs")
    h = hashlib.sha256()
    with a.gguf.open("rb") as f:
        for chunk in iter(lambda: f.read(1024*1024), b""):
            h.update(chunk)
    if h.hexdigest() != report["model_sha256"]:
        p.error("Reference GGUF hash differs")
    from llama_cpp import Llama
    ref = Llama(model_path=str(a.gguf), n_ctx=1024, n_gpu_layers=0,
                n_threads=report["threads"], verbose=False)
    passed = True
    for prompt in dict.fromkeys(row["prompt"] for row in report["runs"]):
        rows = [row for row in report["runs"] if row["prompt"] == prompt]
        reference_prompt = rows[0]["prompt_ids"]
        # Test raw tokenization separately: upstream may insert an explicit BOS.
        alternatives = [ref.tokenize(prompt.encode(), add_bos=b, special=False) for b in (False, True)]
        tokenizer_ok = reference_prompt in alternatives
        ref.reset()
        expected = []
        for token in ref.generate(reference_prompt, temp=0, reset=True):
            if token == ref.token_eos():
                break
            expected.append(token)
            if len(expected) >= report["max_tokens"]:
                break
        for row in rows:
            ok = tokenizer_ok and row["prompt_ids"] == reference_prompt and row["generated_ids"] == expected
            print(json.dumps(dict(prompt=prompt, budget=row["budget"],
                                  tokenizer_parity=tokenizer_ok, greedy_parity=ok),
                             ensure_ascii=False))
            passed &= ok
    return 0 if passed else 1

if __name__ == "__main__":
    raise SystemExit(main())

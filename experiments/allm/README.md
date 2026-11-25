# VoiceDoc / ALLM — experimental dense Qwen3 port

Upstream MIT project: https://github.com/mithun50/ALLM
Pinned revision: 180d3f15f482266d5cafc32a8bc70240c83c5870.
This is actual source adaptation code, not a simulation. Nothing has been compiled, cloned locally or executed when preparing the commit.

## Adaptation
patch_qwen3.py modifies a clean pinned upstream checkout: add required per-head Q/K RMSNorm before RoPE in full and cached attention; read attn_q_norm.weight and attn_k_norm.weight; share the block loader across resident, streamed and prefetched paths; check attention dimensions; add synthetic full-vs-cached test; emit token IDs on stderr. The upstream quantized GGUF matmul, block cache, prefetcher and KV cache remain in use. No third-party engine code is vendored. The intended checkpoint is dense Qwen3-0.6B, not Qwen3-MoE, Qwen3-Next or Qwen3.5. Standard unscaled rotary configuration only; no custom long-context variants.

## Later execution
From this folder with Git, Python 3.10+, and Rust:
- `bash fetch_build.sh` clones and applies the port.
- Or `bash fetch_build.sh --host` on a fresh checkout also compiles host binary.
- Or `ANDROID_NDK_HOME=/path/to/ndk NDK_HOST_TAG=darwin-x86_64 bash fetch_build.sh --android` on a fresh checkout cross-builds for ARM64 API 24 (choose actual host tag).
- After patch-only setup, build manually: `cd upstream && cargo test -p allm-compute && cargo build --locked --release -p allm-cli`. Do not rerun the patch over local changes.
- Obtain a licensed single-file Qwen3-0.6B GGUF independently, first Q8_0 then Q4_K_M subject to Candle support. No model download is automated.
- Run non-medical continuation probes, not chat template or clinical tests:
```sh
python3 run_experiment.py --binary upstream/target/release/allm --model models/qwen3.gguf --output results/host.json --execute
```
Omit --execute to generate plan only. For Android, push ARM64 binary and GGUF via adb to a writable executable directory, chmod binary, use --adb and pass device paths. Optional --serial selects device. This is a native command-line experiment, not JNI/Compose integration.
- Independently install a Qwen3-capable llama-cpp-python version and record its exact revision. Compare same GGUF bytes and greedy token IDs:
```sh
python3 compare_reference.py --report results/host.json --gguf models/qwen3.gguf
```

A successful resident/streamed comparison is necessary but insufficient: both paths may share an architectural bug. Validate tokenizer, reference token IDs and numerical logits across multiple prompts, quantization formats and cache modes before trusting the port. Upstream EOS handling and raw prompt tokenization may still need adaptation for Qwen3 chat templates. No actual results are claimed. Weight cache budget is not total RAM (prefetch buffers, embedding, KV and OS memory are additional). Measure actual peak PSS/RSS, latency, token throughput and temperature on physical Android separately. No production app files are modified.

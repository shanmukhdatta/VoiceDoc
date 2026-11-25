# VoiceDoc / PowerInfer-2 — research experiment (blocked upstream)

Paper: https://arxiv.org/abs/2406.06282
Project: https://powerinfer.ai/v2/
Public repository: https://github.com/Tiiny-AI/PowerInfer
Mobile source request: https://github.com/Tiiny-AI/PowerInfer/issues/194

No verified public PowerInfer-2 mobile source/API or Qwen3 Android artifact was identified. The public original PowerInfer repo is not an implementation of the v2 mobile engine. Do not use it as a drop-in or report it as PowerInfer-2. The original repo can be cloned separately for study with fetch_reference.sh, which does not build or execute inference.

The included schedule_model.py is an independent analytical experiment inspired by overlapping I/O and computation. Given a CSV of hypothetical or externally measured per-step load and compute durations, it calculates serial vs ideal two-stage overlapped makespan. It does not execute a neural network, predict sparse activations, implement heterogeneous kernels, enforce RAM limits, or produce tokens. It is not a reproduction or benchmark of PowerInfer-2. Dense Qwen3-0.6B must not be assumed to have the activation sparsity used by that paper.

Later, obtain an authentic v2 source or choose an explicitly different engine. Verify model/quantization compatibility, source license, ARM64 build, dense-model correctness and reproducible output parity before measuring inference. Do not enable this in production VoiceDoc.

Optional analytical example (not device data):
```sh
python3 schedule_model.py example_trace.csv
```
CSV columns: step,load_ms,compute_ms. Each row is one ordered stage. One I/O worker and one compute worker, unlimited intermediate buffering, no contention. This is an idealized lower-bound schedule for these durations, not a device performance prediction.

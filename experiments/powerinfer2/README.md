# VoiceDoc / PowerInfer-2-inspired Qwen3 inference prototype

Paper: https://arxiv.org/abs/2406.06282
Project: https://powerinfer.ai/v2/
Public original PowerInfer: https://github.com/Tiiny-AI/PowerInfer

This is independently authored code, not a copy or reproduction of the unavailable v2 mobile engine. It executes real dense Qwen3 inference with Hugging Face transformer attention, including Q/K normalization, and streams weights from local safetensors. It is a desktop/CPU research reference, not an Android native engine. No commands, model download, inference or tests were executed when adding these files.

## Modes
- resident: conventional complete model, reference for parity.
- serial: load one transformer block, compute, release.
- prefetch: load next transformer block concurrently with current block computation.
- cluster: same attention weight prefetch; partition the SwiGLU FFN by intermediate neurons, read gate/up rows and down columns on demand, accumulate all partial outputs and prefetch one upcoming cluster. No sparse approximation or skipped neurons.

PowerInfer-2 uses finer-grained neuron-cluster pipelining, segmented cache and heterogeneous CPU/NPU scheduling. This prototype borrows the I/O-compute overlap and cluster partition concepts only. It does NOT implement learned sparsity, hot/cold cache policy, NPU scheduling, INT4 kernels or Android integration. Resident embeddings/head, KV cache, activation buffers, OS page cache and temporary allocations remain; no global memory cap is guaranteed. Cluster accumulation may change floating-point rounding. Python threading/storage contention may yield worse speed.

## Run later (Mac/Linux host)
Use a fresh Python 3.10+ environment. Obtain a licensed local Qwen3-0.6B Hugging Face safetensors checkpoint (or merged SFT checkpoint), tokenizer and config independently. No downloads are initiated by the runner.
```sh
python -m pip install -r requirements.txt
python -m unittest test_cluster.py
for mode in resident serial prefetch cluster; do
  python stream_qwen.py --model /path/to/Qwen3-0.6B --mode "$mode" --output "results/$mode.json"
done
python compare_runs.py results/resident.json results/serial.json results/prefetch.json results/cluster.json
```
Run with identical checkpoint, prompt, dtype and threads in separate processes. Run multiple English/Telugu prompts, check greedy IDs and later compare logits with tolerance. A parity pass on one prompt does not establish correctness. Use external process memory measurements and cold/warm repetitions; output timings are wall-clock only. BF16 may be slow or unsupported on some host CPUs. If the prototype passes, design native Android implementation with matched kernels and parity before benchmarking phone memory and latency.

Existing fetch_reference.sh clones only the original v1 for study; schedule_model.py is an optional analytical schedule, not inference. No production medical answers or Android changes.

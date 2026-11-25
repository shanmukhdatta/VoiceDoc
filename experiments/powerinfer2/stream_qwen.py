"""Independent PowerInfer-2-inspired dense Qwen3 CPU reference, not upstream v2."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import time

import torch
from torch import nn
from torch.nn import functional as F
from accelerate import init_empty_weights
from safetensors import safe_open
from transformers import AutoConfig, AutoTokenizer, Qwen3ForCausalLM

class Weights:
    def __init__(self, root, dtype):
        self.root, self.dtype = Path(root).resolve(), dtype
        index = self.root / "model.safetensors.index.json"
        if index.is_file():
            self.files = json.loads(index.read_text())["weight_map"]
        else:
            with safe_open(str(self.root / "model.safetensors"), framework="pt", device="cpu") as f:
                self.files = {k: "model.safetensors" for k in f.keys()}

    def read(self, name, region=None):
        path = (self.root / self.files[name]).resolve()
        if not path.is_relative_to(self.root):
            raise ValueError("Weight path escapes model directory")
        with safe_open(str(path), framework="pt", device="cpu") as f:
            tensor = f.get_tensor(name) if region is None else f.get_slice(name)[region]
            # Own the active weight buffer; do not retain mapped shard views.
            return tensor.to(dtype=self.dtype, copy=True).contiguous()

def put(module, name, tensor):
    parent, _, leaf = name.rpartition(".")
    target = module.get_submodule(parent) if parent else module
    old = getattr(target, leaf)
    if old.shape != tensor.shape:
        raise ValueError(f"Shape mismatch: {name}")
    setattr(target, leaf, nn.Parameter(tensor, requires_grad=False))

class ClusterMLP(nn.Module):
    """Exact dense SwiGLU partition; every cluster executes, no pruning."""
    def __init__(self, store, prefix, width, chunk, executor):
        super().__init__()
        self.store, self.prefix = store, prefix
        self.width, self.chunk, self.executor = width, chunk, executor

    def load(self, lo, hi):
        get = self.store.read
        p = self.prefix
        return (get(p + ".gate_proj.weight", (slice(lo, hi), slice(None))),
                get(p + ".up_proj.weight", (slice(lo, hi), slice(None))),
                get(p + ".down_proj.weight", (slice(None), slice(lo, hi))))

    def forward(self, x):
        spans = [(lo, min(lo + self.chunk, self.width))
                 for lo in range(0, self.width, self.chunk)]
        future = self.executor.submit(self.load, *spans[0])
        total = torch.zeros_like(x, dtype=torch.float32)
        for i in range(len(spans)):
            gate, up, down = future.result()
            # One future cluster can load while current cluster computes.
            future = (self.executor.submit(self.load, *spans[i+1])
                      if i+1 < len(spans) else None)
            hidden = F.silu(F.linear(x, gate)) * F.linear(x, up)
            total.add_(F.linear(hidden, down).float())
            del gate, up, down, hidden
        return total.to(x.dtype)

class StreamEngine:
    def __init__(self, root, dtype, mode, cluster):
        config = AutoConfig.from_pretrained(root, local_files_only=True, trust_remote_code=False)
        if config.model_type != "qwen3" or config.hidden_act != "silu":
            raise ValueError("Only dense Qwen3 SwiGLU supported")
        config._attn_implementation = "eager"
        self.store = Weights(root, dtype)
        self.io = ThreadPoolExecutor(max_workers=1)
        self.cluster_io = ThreadPoolExecutor(max_workers=1)
        self.mode = mode
        self.future = None
        self.handles = []
        try:
            # Parameters on meta; rotary buffers initialized normally on CPU.
            with init_empty_weights(include_buffers=False):
                self.model = Qwen3ForCausalLM(config)
            self.model.eval()
            self.model.requires_grad_(False)
            for name in ("model.embed_tokens.weight", "model.norm.weight"):
                put(self.model, name, self.store.read(name))
            if config.tie_word_embeddings:
                self.model.lm_head.weight = self.model.model.embed_tokens.weight
            else:
                put(self.model, "lm_head.weight", self.store.read("lm_head.weight"))
            self.layers = self.model.model.layers
            self.names = []
            for i, layer in enumerate(self.layers):
                if mode == "cluster":
                    layer.mlp = ClusterMLP(self.store, f"model.layers.{i}.mlp",
                                           config.intermediate_size, cluster, self.cluster_io)
                self.names.append(tuple(n for n, _ in layer.named_parameters()))
                self.handles.append(layer.register_forward_pre_hook(self.before(i)))
                self.handles.append(layer.register_forward_hook(self.after(i), always_call=True))
        except BaseException:
            self.close()
            raise

    def load(self, i):
        return {name: self.store.read(f"model.layers.{i}.{name}") for name in self.names[i]}

    def before(self, i):
        def hook(layer, args):
            data = self.future.result() if self.future is not None else self.load(i)
            self.future = None
            for name, tensor in data.items():
                put(layer, name, tensor)
            if self.mode != "serial" and i+1 < len(self.layers):
                self.future = self.io.submit(self.load, i+1)
        return hook

    def after(self, i):
        def hook(layer, args, output):
            for name, param in tuple(layer.named_parameters()):
                if param.device.type != "meta":
                    put(layer, name, torch.empty_like(param, device="meta"))
        return hook

    @torch.no_grad()
    def step(self, ids, cache):
        if self.future is not None:
            raise RuntimeError("Previous forward did not complete; discard engine")
        if self.mode != "serial":
            self.future = self.io.submit(self.load, 0)
        return self.model(input_ids=ids, past_key_values=cache, use_cache=True,
                          logits_to_keep=1, return_dict=True)

    def close(self):
        for handle in self.handles:
            handle.remove()
        self.io.shutdown(wait=True, cancel_futures=True)
        self.cluster_io.shutdown(wait=True, cancel_futures=True)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--model", required=True, help="Local complete Qwen3 safetensors directory")
    p.add_argument("--mode", choices=("serial", "prefetch", "cluster", "resident"), default="cluster")
    p.add_argument("--dtype", choices=("float32", "bfloat16"), default="float32")
    p.add_argument("--cluster", type=int, default=256)
    p.add_argument("--threads", type=int, default=4)
    p.add_argument("--prompt", default="Explain why the sky appears blue in one sentence.")
    p.add_argument("--max-tokens", type=int, default=16)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    if min(a.cluster, a.threads, a.max_tokens) < 1 or a.max_tokens > 64:
        p.error("Positive cluster, threads and token limit (max 64) required")
    torch.set_num_threads(a.threads)
    dtype = getattr(torch, a.dtype)
    tok = AutoTokenizer.from_pretrained(a.model, local_files_only=True, trust_remote_code=False)
    ids = tok.apply_chat_template([{"role": "user", "content": a.prompt}],
                                  tokenize=True, add_generation_prompt=True,
                                  enable_thinking=False, return_tensors="pt")
    if ids.shape[1] > 512:
        p.error("Use a short prompt (at most 512 tokens)")
    engine = None
    start = time.perf_counter()
    try:
        if a.mode == "resident":
            model = Qwen3ForCausalLM.from_pretrained(
                a.model, local_files_only=True, torch_dtype=dtype,
                attn_implementation="eager").eval()
            def step(x, cache):
                return model(input_ids=x, past_key_values=cache, use_cache=True,
                             logits_to_keep=1, return_dict=True)
        else:
            engine = StreamEngine(a.model, dtype, a.mode, a.cluster)
            step = engine.step
        load_seconds = time.perf_counter() - start
        eos = model.generation_config.eos_token_id if engine is None else (
            engine.model.generation_config.eos_token_id)
        if eos is None:
            eos = tok.eos_token_id
        eos = set(eos if isinstance(eos, list) else [eos])
        cache = None
        tokens = []
        first = None
        run_start = time.perf_counter()
        with torch.no_grad():
            for _ in range(a.max_tokens):
                out = step(ids, cache)
                cache = out.past_key_values
                new = int(out.logits[0, -1].argmax())
                tokens.append(new)
                if first is None:
                    first = time.perf_counter() - run_start
                if new in eos:
                    break
                ids = torch.tensor([[new]], dtype=torch.long)
        result = dict(engine="independent_dense_reference", mode=a.mode,
                      dtype=a.dtype, cluster=a.cluster, threads=a.threads,
                      prompt=a.prompt, token_ids=tokens, max_tokens=a.max_tokens,
                      text=tok.decode(tokens, skip_special_tokens=True),
                      load_seconds=load_seconds, first_token_seconds=first,
                      generation_seconds=time.perf_counter()-run_start,
                      validation="unvalidated")
    finally:
        if engine is not None:
            engine.close()
    a.output.parent.mkdir(parents=True, exist_ok=True)
    with a.output.open("x", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    main()

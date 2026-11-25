"""Apply the VoiceDoc dense-Qwen3 port to the exact pinned ALLM checkout."""
import argparse
from pathlib import Path
import re
import subprocess

REV = "180d3f15f482266d5cafc32a8bc70240c83c5870"
MARKER = "// VoiceDoc Qwen3 port"

def replace(text, old, new, count=1):
    if text.count(old) != count:
        raise ValueError(f"Expected {count} occurrences of {old!r}")
    return text.replace(old, new)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("checkout", type=Path)
    a = p.parse_args()
    root = a.checkout.resolve()
    def git(*args):
        return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()
    if git("rev-parse", "HEAD") != REV:
        p.error("Wrong upstream revision; refusing to patch")
    src = root / "crates/allm-compute/src"
    if git("status", "--porcelain"):
        p.error("Checkout must be clean. Use a fresh pinned clone.")
    changes = {}
    block = src / "block.rs"
    s = block.read_text()
    s = replace(s, "    pub v_bias: Option<Tensor>,",
        "    pub v_bias: Option<Tensor>,\n    " + MARKER +
        "\n    pub q_norm: Option<RmsNorm>,\n    pub k_norm: Option<RmsNorm>,")
    anchor = "        let q = apply_rope(&q, &cos, &sin)?;"
    s = replace(s, anchor,
        "        let q = match &self.q_norm { Some(n) => n.forward(&q)?, None => q };\n"
        "        let k = match &self.k_norm { Some(n) => n.forward(&k)?, None => k };\n" + anchor, 2)
    changes[block] = s
    loader = src / "loader.rs"
    s = loader.read_text()
    anchor = '        v_bias: load_opt_bias(content, file, &format!("{p}.attn_v.bias")),'
    s = replace(s, anchor, anchor + """
        q_norm: if config.architecture == "qwen3" {
            Some(load_head_norm(content, file, &format!("{p}.attn_q_norm.weight"), config)?)
        } else { None },
        k_norm: if config.architecture == "qwen3" {
            Some(load_head_norm(content, file, &format!("{p}.attn_k_norm.weight"), config)?)
        } else { None },""")
    anchor = "/// Load a full transformer block"
    s = replace(s, anchor, """fn load_head_norm(content: &Content, file: &mut File, name: &str,
                  config: &ModelConfig) -> Result<RmsNorm> {
    let weight = load_dense(content, file, name)?;
    if weight.dims() != [config.head_dim] {
        return Err(Error::invalid_model(format!("bad Qwen3 head norm shape: {name}")));
    }
    Ok(RmsNorm::new(weight, config.rms_eps))
}

""" + anchor)
    changes[loader] = s
    # Use the same loader for resident, streamed and prefetched blocks.
    model = src / "model.rs"
    s = model.read_text()
    start = s.index("            let p = format!(\"blk.{i}\");")
    end = s.index("\n        }\n\n        let final_norm", start)
    s = s[:start] + "            blocks.push(crate::loader::load_block(&content, &mut file, &config, i)?);" + s[end:]
    changes[model] = s
    cfg = src / "config.rs"
    s = cfg.read_text()
    anchor = "        // Vocab size: prefer explicit metadata"
    s = replace(s, anchor, """        if arch == "qwen3" {
            if n_heads == 0 || n_kv_heads == 0 || n_heads % n_kv_heads != 0
                || head_dim == 0 || head_dim % 2 != 0
                || get_u64("attention.value_length").unwrap_or(head_dim as u64) != head_dim as u64
                || get_u64("rope.dimension_count").unwrap_or(head_dim as u64) != head_dim as u64
            {
                return Err(Error::invalid_model("unsupported Qwen3 attention dimensions"));
            }
        }

""" + anchor)
    changes[cfg] = s
    # Initialize new optional fields in existing synthetic Attention constructors.
    for path in (root / "crates").rglob("*.rs"):
        if path in changes:
            continue
        old = path.read_text()
        new = re.sub(r"(?m)^(\s*)v_bias: None,$",
                     r"\1v_bias: None,\n\1q_norm: None,\n\1k_norm: None,", old)
        if old != new:
            changes[path] = new
    test = src / "block/tests.rs"
    s = changes[test]
    s += """

#[test]
fn voicedoc_qwen3_head_norm_cached_matches_full() {
    let dev = Device::Cpu;
    let hidden = 8;
    let hd = 4;
    let attn = Attention {
        q_proj: dense(12, hidden, &dev),
        k_proj: dense(4, hidden, &dev),
        v_proj: dense(4, hidden, &dev),
        o_proj: dense(hidden, 12, &dev),
        q_bias: None, k_bias: None, v_bias: None,
        q_norm: Some(RmsNorm::new(Tensor::from_slice(&[0.5f32, 1.0, 1.5, 2.0], hd, &dev).unwrap(), 1e-6)),
        k_norm: Some(RmsNorm::new(Tensor::from_slice(&[2.0f32, 1.5, 1.0, 0.5], hd, &dev).unwrap(), 1e-6)),
        n_heads: 3, n_kv_heads: 1, head_dim: hd,
    };
    let rope = RopeCache::new(hd, 16, 1000000.0, &dev).unwrap();
    let x = Tensor::randn(0f32, 1f32, (3, hidden), &dev).unwrap();
    let full = attn.forward(&x, &rope, 0).unwrap();
    let mut cache = KvCache::default();
    let parts: Vec<Tensor> = (0..3).map(|i|
        attn.forward_cached(&x.narrow(0, i, 1).unwrap(), &rope, &mut cache).unwrap()
    ).collect();
    let cached = Tensor::cat(&parts, 0).unwrap();
    assert!(max_abs_diff(&full, &cached) < 1e-3);
}
"""
    changes[test] = s
    cli = root / "cli/src/commands/run.rs"
    s = cli.read_text()
    s = replace(s, "    let prompt_tokens = ids.len();",
        "    let prompt_tokens = ids.len();\n    eprintln!(\"VOICEDOC_PROMPT_IDS={ids:?}\");")
    s = replace(s, "    let mut gen_count = 0usize;",
        "    let mut gen_count = 0usize;\n    let mut emitted_ids: Vec<u32> = Vec::new();")
    s = replace(s, "            gen_count += 1;",
        "            gen_count += 1;\n            emitted_ids.push(tok);")
    s = replace(s, "    // Timing report on stderr.",
        "    eprintln!(\"VOICEDOC_GENERATED_IDS={emitted_ids:?}\");\n    // Timing report on stderr.")
    changes[cli] = s
    # Prepare every replacement before writing; never patch an unknown revision.
    for path, content in changes.items():
        path.write_text(content, encoding="utf-8")
        print(path.relative_to(root))
    print("Patch written, not compiled or tested.")

if __name__ == "__main__":
    main()

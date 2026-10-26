# Day 09 — Fine-tuning, export, quantization and hardware acceleration

**Date:** 2026-11-03  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Continue implementing the [full 40-section README](../../README.md).  
**Outcome:** Build the complete off-device experimentation pipeline and mobile optimization workflow; run feasible experiments and document unavailable hardware honestly.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. Leave a checkbox unchecked until the stated action is actually verified. Day numbers represent the intended order, not permission to skip unfinished safety-critical work. **Never claim clinical reliability or published availability without validation.**

## Task A — Learn before coding

- [ ] **01.** Study model licensing, dataset curation, instruction tuning, train/dev/test splits and leakage.
- [ ] **02.** Read Hugging Face Transformers, Datasets, PEFT LoRA/QLoRA, Accelerate and quantization basics.
- [ ] **03.** Explain why fine-tuning styles/behaviors and RAG provides traceable factual reference knowledge.
- [ ] **04.** Study calibration, quantization quality regression and export formats for compatible Android runtimes.
- [ ] **05.** Read CPU/GPU/NPU backend selection, Qualcomm QNN compatibility requirements and Android profiling.

## Task B — Setup and environment

- [ ] **01.** Create ml/datasets, preprocessing, training, evaluation, quantization, conversion, export and notebook paths.
- [ ] **02.** Use a Kaggle/Colab/local GPU only if legitimately available; record environment, quota, model license and actual GPU RAM.
- [ ] **03.** Pin Python/PyTorch/Transformers/PEFT dependencies and record versions/seed.
- [ ] **04.** Select appropriately licensed instruction datasets; exclude patient information and unsupported clinical claims.
- [ ] **05.** Prepare small separate train/dev/test suites with language and safety labels; avoid duplicates/leakage.
- [ ] **06.** Confirm chosen base checkpoint, tokenizer, training recipe and deployable export path are mutually compatible.

## Task C — Small isolated exercise

- [ ] **01.** Run a small nonmedical LoRA tutorial with a tiny licensed model and few safe instruction samples.
- [ ] **02.** Compare zero-shot before/after performance on withheld tasks and inspect loss/quality plots.
- [ ] **03.** Quantize a compatible small model and compare size/latency/output on a few harmless prompts.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Write dataset validation, schema and license checking scripts; log rejected rows.
- [ ] **02.** Implement train/test grouping, deduplication, seed control and data manifests.
- [ ] **03.** Create parameterized LoRA/QLoRA training config and reusable training/evaluation scripts.
- [ ] **04.** Add checkpoint saving/resume, GPU memory logging, training metrics and safe interruption.
- [ ] **05.** Evaluate baseline and adapted models on language, response form, groundedness and safety; note clinically unreviewed metrics.
- [ ] **06.** Implement merging/adaptation/export steps only if actually supported by selected backend.
- [ ] **07.** Implement quantization/conversion pipeline toward chosen LiteRT-LM runtime; validate tokenizer and operators.
- [ ] **08.** Build a SHA-256 model manifest, model card and reproducible package creation instructions.
- [ ] **09.** Load resulting artifact into Android app on a real device and verify genuine generation; otherwise document failure precisely.
- [ ] **10.** Profile model load, TTFT, tokens/s, peak RAM and battery/thermal conditions.
- [ ] **11.** Implement CPU/GPU capability detection and fallback; add QNN path only for actually supported Snapdragon device+model+runtime.
- [ ] **12.** Compare baseline, adapted and quantized models using the SAME held-out test set.
- [ ] **13.** Never commit raw training datasets, keys or large model weights directly to regular Git.

## Task E — Experiments / learn by comparison

- [ ] **01.** Run baseline vs LoRA adaptation if GPU and export path permit; attach numeric results, not predictions.
- [ ] **02.** Compare FP/quantized quality on golden prompts and note regressions.
- [ ] **03.** Compare CPU/GPU/NPU modes where actually available; mark unsupported modes N/A.
- [ ] **04.** Measure prompt length and top-K retrieval effects on latency and memory.

## Task F — Testing and verification

- [ ] **01.** Dataset train/test leakage and license validation scripts pass.
- [ ] **02.** Training code is executable, reproducible and actually run where resources allow.
- [ ] **03.** Only model files that load and generate on-device are marked deployable.
- [ ] **04.** Quantization is rejected when safety/quality regression is unacceptable.
- [ ] **05.** Unsupported GPU/NPU paths fall back cleanly or report unavailable.

## Task G — Proof of completion

- [ ] **01.** Reproducible dataset manifests, training scripts/notebook, checkpoint metadata and evaluations.
- [ ] **02.** Conversion/quantization scripts, mobile model card and hardware benchmark comparison.
- [ ] **03.** Actual run logs or explicit BLOCKED entries with reason and reproduction steps; commit SHA.
- [ ] Record device/OS specs, model/dependency versions, commands and results in `docs/learning/day-09-notes.md`.
- [ ] Explain in your own words what you built, the logic, assumptions, limits and what remains broken.
- [ ] Inspect `git diff` and `git status`, run tests and commit only verified changes as `day 09: <actual completed work>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) based on evidence.

## Exit criteria (mandatory)

Training+optimization engineering is implemented and bench-tested where resources permit; no claim of a fine-tuned deployed model unless confirmed on the phone.

## Official learning references

- [Hugging Face PEFT](https://huggingface.co/docs/peft/index)
- [Transformers training](https://huggingface.co/docs/transformers/training)
- [LiteRT-LM project](https://github.com/google-ai-edge/LiteRT-LM)
- [Qualcomm AI Hub](https://aihub.qualcomm.com/)

## When blocked

1. Keep the exact failing command, stack trace, hardware/software info and expected result.
2. Ask AI for an explanation plus one hint, then attempt the change yourself.
3. Create a minimal reproduction, verify the fix, rerun tests and record the outcome.
4. If resources or data aren't available, label the affected task **BLOCKED** with evidence; continue independent preparation, but do not count a mock as a real subsystem.

---

**Next:** [Day 10](DAY_10.md)

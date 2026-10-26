# Day 05 — On-device quantized SLM and real generated answers

**Date:** 2026-10-30  
**Status:** ⬜ Not started / 🟨 In progress / ✅ Evidence verified (edit manually)  
**Scope:** Implement the [40-section README](../../README.md) gradually, not a fake demo.  
**Outcome:** Prove that a real quantized SLM can generate text on the target Android phone, fed by local RAG evidence.

> **Student workflow:** learn → mini-experiment → implement real component → test → explain → commit. The checkboxes are your personal tracker; leave unchecked until a real result exists. Learning hours and tests may extend past the calendar day; never mark unverified work complete.

## Task A — Learn before coding

- [ ] **01.** Read tokenizer basics, generation loop, KV cache, TTFT and tokens/second.
- [ ] **02.** Compare FP16, INT8 and INT4; understand quality/speed/RAM tradeoffs rather than assuming smaller is always better.
- [ ] **03.** Read the official LiteRT-LM Android Kotlin API examples and supported model formats.
- [ ] **04.** Learn suspend initialization, streaming Flow collection, cancellation and resource lifecycle.
- [ ] **05.** Study prompt-injection threats: retrieved content is evidence, not instructions.

## Task B — Setup and environment

- [ ] **01.** Check free phone storage, RAM, ABI, chipset, Android version and support for chosen runtime backend.
- [ ] **02.** Pick a license-compatible ~1B quantized instruction model in a supported `.litertlm` artifact, confirm actual availability and tokenizer/runtime compatibility.
- [ ] **03.** Record selected model's exact source/version/hash/size/license; avoid uploading model binaries to normal Git.
- [ ] **04.** Pin compatible Gradle inference dependency to a tested version instead of `latest.release`.
- [ ] **05.** Before touching the main app, build an official sample on the same physical phone.

## Task C — Small isolated exercise

- [ ] **01.** Run a real quantized model in a small sample app with a harmless prompt.
- [ ] **02.** Measure model-load time, TTFT and generated tokens/second for three short prompts.
- [ ] **03.** Trigger generation cancellation and verify memory/native resources are released.

## Task D — Implement inside VoiceDoc

- [ ] **01.** Implement `LanguageModelEngine` interface with LiteRT-LM adapter and explicit Engine lifecycle.
- [ ] **02.** Add load/unload/ready/error states and display model unavailability accurately.
- [ ] **03.** Stream generated text from Kotlin Flow to ViewModel without blocking the UI thread.
- [ ] **04.** Implement a bounded PromptBuilder: safety rules, language, user query, trusted chunks, minimal session history and required output structure.
- [ ] **05.** Separate source text delimiters from untrusted user messages and strip injected instructions from document presentation where possible.
- [ ] **06.** Ensure model prompt references the exact retrieved sources and does not invent citations.
- [ ] **07.** Connect typed query → language normalizer → retrieval → prompt → local SLM.
- [ ] **08.** Implement cancel, memory-low, model-corrupt and unavailable-backend behavior.
- [ ] **09.** Record backend selected, model hash, prompt length and generation latency without logging raw health text.
- [ ] **10.** Run the entire typed path in airplane mode on a real phone.
- [ ] **11.** Implement quality checks comparing model answer to evidence and no-evidence fallback.

## Task E — Experiments / learn by comparison

- [ ] **01.** Compare a short and long prompt using the same question for latency, memory and answer grounding.
- [ ] **02.** Compare Q4/INT4 and other genuinely supported model variants if available; report measured quality regression.
- [ ] **03.** Disable retrieval and demonstrate why generated responses may become less grounded, using safe nonclinical test questions.

## Task F — Testing and verification

- [ ] **01.** Model runs with network fully disabled after provisioning.
- [ ] **02.** No cloud inference or external API call is hidden in the application.
- [ ] **03.** Answers are not labelled medically verified merely because they are generated.
- [ ] **04.** Wrong/missing model file fails safely with clear UI message.
- [ ] **05.** Generation can be cancelled with no stuck UI or leaked engine handle.

## Task G — Proof of completion

- [ ] **01.** Working LiteRT-LM adapter, prompt builder and end-to-end typed query/answer pipeline.
- [ ] **02.** Device benchmark log, exact model/license/checksum manifest, golden test outputs and source citations.
- [ ] **03.** Unit and integration tests with actual successful device run evidence.
- [ ] Record exact versions, device/OS specs, commands run and logs in `docs/learning/day-05-notes.md`.
- [ ] Write three sentences in your own words: what you built, how it works, and what remains broken.
- [ ] Run `git status`, review your diff, run relevant tests, then commit with `day 05: <verified result>`.
- [ ] Update the [master progress tracker](../../IMPLEMENTATION_PLAN.md) accurately.

## Exit criteria (mandatory)

Only mark complete after **real on-device inference**; mocked generation is not acceptance evidence.

## Official learning references

- [LiteRT-LM Android guide](https://developers.google.com/edge/litert-lm/android)
- [LiteRT-LM upstream](https://github.com/google-ai-edge/LiteRT-LM)
- [LiteRT-LM Android implementation guide](https://developer.android.com/agents/skills/device-ai/litert/litert-lm/skill)

## When blocked

1. Preserve the failing command, console output, device/runtime details and the expected result.
2. Ask AI to explain the error and give **one hint**; try your own fix first.
3. Write a minimal reproduction, apply a verified fix, rerun tests and document the result.
4. If model/hardware/data access is impossible, mark **BLOCKED** with evidence and use a clearly labelled interface-level fallback for development; do **not** claim the real feature works.

---

**Next:** [Day 06](DAY_06.md).

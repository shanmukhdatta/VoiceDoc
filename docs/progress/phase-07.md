# Phase 07 — Offline text pipeline
**STATUS: Testable orchestration logic implemented; no production SLM inference.**

- Flow: independent bilingual input safety -> retrieval -> approved model structured generation -> output source-ID validation -> evidenced response or static abstention.
- Urgent examples bypass retriever/model. Missing KB, local LLM or invalid output never invent medical responses.
- JVM coroutine tests for escalation precedence, retrieval failures, uninstalled model, unsupported citations, and synthetic positive path.
- The only included production generator is `UnavailableStructuredGenerator`, which deliberately refuses generation. Test fakes never ship as runtime health engines.
- Remaining gates: real tokenizer, ONNX embedder, LiteRT-LM Gemma runtime and model rights, reviewed corpus, claim-level factuality, medical safety validation, bilingual benchmarks.

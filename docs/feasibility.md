# Voice Doctor AI — Phase 00 feasibility baseline
Date: 2026-10-13 · Review branch: phase-00/feasibility-baseline
**Decision: CONDITIONALLY FEASIBLE architecture, NOT YET VALIDATED END-TO-END. Phase 00 is AWAITING VERIFICATION.**

## Product contract
Offline Android **health-information** assistant with Telugu and English speech/text I/O, medical knowledge retrieval, quantized local small language model, standalone bilingual safety routing, source citations and optional encrypted history. It does **not** autonomously diagnose, prescribe, replace clinicians, or reliably exclude emergencies. No Tamil in initial scope. Internet is not allowed for runtime inference; offline first-install/distribution is a separate future delivery decision.

## Repository audit
As of 2026-10-13, default `main` tree contains **README.md only**, a 40 KB architecture proposal. No Gradle project, CI, APK, trained models, medical corpus, tests, local benchmark evidence, or selected dependency lockfiles. README references three initial languages; this baseline overrides that to **te/en only**. We intentionally keep `main` and the README unchanged for review.

## Main feasibility result
1. **Established upstream feasibility:** Kotlin/Compose/SQLite Android app; whisper.cpp Android, Silero ONNX, ONNX Runtime Android, LiteRT-LM Kotlin with Gemma 3 1B demonstration, general Android TextToSpeech API, sherpa-onnx Android TTS framework.
2. **Feasible but not yet integrated:** E5-small ONNX text pipeline including tokenizer and masked pooling; exact+lexical RAG; sequential model loading and lifecycle handling; bilingual deterministic safety routing; checked versioned knowledge packs.
3. **High-risk:** accurate Telugu medical/code-switched ASR; Gemma 1B Telugu medically grounded generation; native memory contention/latency/heat; NPU backend/model compatibility; license-compliant offline Telugu TTS.
4. **Open release blocker:** verified reliable, commercial-compatible **offline Telugu TTS** on target device OR redistributable mobile voice model. Android framework support is not evidence the phone contains a Telugu offline voice.
5. **Medical quality is not proved:** engineering tests are not clinical validation; bilingual domain experts/qualified clinical reviewers must evaluate before real-health reliance.

## Compatibility evidence matrix
| Path | Official source | What is actually established | Remaining gate |
|---|---|---|---|
| Gemma 3 1B + LiteRT-LM | [Kotlin API](https://developers.google.com/edge/litert-lm/android), [Gemma LiteRT](https://huggingface.co/litert-community/Gemma3-1B-IT) | .litertlm load API, CPU/GPU/NPU backends and reference Android memory benchmarks | exact artifact & Kotlin AAR load on actual phone, rights, RAM/latency |
| QNN NPU | [Google Qualcomm](https://developers.google.com/edge/litert/next/qualcomm) | supported device families and NPU build route | target SoC, QNN libs, AOT package, Android ABI/driver |
| Whisper multilingual | [whisper.cpp](https://github.com/ggml-org/whisper.cpp), [model sizes](https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md) | Android example; tiny 75 MiB, base 142 MiB unquantized GGML | Telugu health audio accuracy; CPU time and RAM |
| Silero VAD | [Silero](https://github.com/snakers4/silero-vad) | tiny MIT ONNX VAD, 8/16 kHz | on-device latency, stateful detection quality |
| IndicConformer | [AI4Bharat](https://huggingface.co/ai4bharat/indic-conformer-600m-multilingual) | MIT checkpoint, Telugu supported, ONNX indicated | custom op, tokenizer/decoder port; size too high for default |
| E5-small | [E5 weights](https://huggingface.co/intfloat/multilingual-e5-small), [ORT Android](https://onnxruntime.ai/docs/build/android.html) | MIT, ONNX artifacts, 384 dimensions and Android ONNX framework | model graph/tokenizer, device RAM, Telugu retrieval quality |
| TTS | [Android Voice API](https://developer.android.com/reference/android/speech/tts/Voice), [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) | query offline installed voices; Android JNI TTS backend | actual Telugu offline synth + redistribution-capable fallback |
| Telugu voice license | [MMS Telugu](https://huggingface.co/facebook/mms-tts-tel), [IndicF5](https://huggingface.co/ai4bharat/IndicF5) | MMS CC-BY-NC-4.0 excludes commercial baseline; IndicF5 MIT but native PyTorch reference | deployable compact ONNX + phonemization/license audited |
| Knowledge reuse | [MedlinePlus terms](https://medlineplus.gov/about/using/usingcontent/) | many NLM topic summaries reusable with attribution; other linked/drug/encyclopedia content restricted | **item-level** source audit; Telugu translation rights and review |

## Memory and storage budget — hypothesis, NOT a benchmark
Use **decimal MB** in table unless explicitly MiB. Assets are preliminary; final quantized/exported artifact and OS voice sizes MUST be measured.

| Asset | Disk example | Evidence type |
|---|---:|---|
| Gemma3 1B quantized QAT reference | **529 MB** | published LiteRT example artifact, specific variant |
| Whisper multilingual base unquantized | **142 MiB** (~149 MB) | upstream ggml official table |
| Silero VAD | **~2 MB** | upstream JIT size; exported ONNX can differ |
| multilingual E5 small | **471 MB** FP32 weights, plus tokenizer; mobile optimized target TBD | upstream model weights size; **not a quantized mobile guarantee** |
| KB and source metadata | **20–100 MB** | planning-only assumption based on corpus |
| 5K x 384 FP32 document embeddings | **~7.68 MB** + index overhead | mathematical raw vector calculation |
| English TTS | **0 additional voice MB if OS-installed**, otherwise TBD | vendor dependent |
| Telugu TTS | **TBD** | release blocker; no chosen license-compatible artifact |
| App/native runtimes, tokenizer/cache | **~60–200 MB** | rough planning reserve, not measured |
| **Known/example disk subtotal (excluding Telugu voice)** | **~1.24–1.44 GB** | computed illustrative range, NOT final APK |
| Extra free space needed for downloads/staging | **~1–2 GB** | deployment planning only |

**Recalculation:** 529 + 149 + 2 + 471 + (20 to 100) + 7.68 + (60 to 200) ≈ **1239–1459 MB** of reference assets/allowance. Not an exact file bundle; quantizing E5 could reduce it, other model artifacts could increase it. Reserve **~2 GB minimum model storage plus >= 4–5 GB free for tests**, and record install/asset extraction overhead. Never promise a single tiny offline APK; use an auditable local model pack.

**RAM:** Google's published Gemma 1B QAT 2048-context S24 Ultra test reports **1009 MB CPU RSS** and **1205 MB CPU RSS in GPU configuration** for LiteRT-LM. whisper.cpp README gives approximately **388 MB** memory for base under its own benchmark conditions. Those are **different conditions/platforms** and must not be added as a measured concurrent peak. E5 ONNX session, Android UI, SQLite, TTS and model cache allocate additional RAM. **Planning envelope for sequential pipeline: ~1.5–3 GiB process memory** (unverified). Prefer closing ASR session before generation if necessary, keeping E5 loads scoped, and enforcing low-memory cleanup; Android 8 GiB device preferred for first evaluation, 6 GiB experimental. Cold-start and lifecycle reload costs must be measured.

## Retrieval + safety feasibility gates
- Collect provenance and rights before ingesting a knowledge source. Do NOT assume that a reputable publicly visible website permits embedding or redistribution. MedlinePlus distinguishes US-government/public-domain summaries from third-party encyclopedia and medication material. Record licenses per **individual** source, not just website.
- Document English and professionally reviewed Telugu translations with license, review date, version and content hash.
- Curate an English/Telugu labeled retrieval set (including cross-language queries), assess Recall@3/5, nDCG and evidence sufficiency; tune fusion and abstention thresholds on validation data; hold out evaluation cases.
- Pre-input safety router: high-risk/urgent language must trigger reviewed, general seek-help escalation without dependence on successful ASR/LLM. If ASR uncertain, ask for on-screen transcript confirmation before high-impact output. Don't tell user a condition has been ruled out.
- Output: fail closed on malformed output, missing citation, unsupported claims, disallowed personalized advice or dangerous reassuring text. A retrieved ID is not evidence without statement-level support verification.
- Clinician review required for safety-case coverage, false reassurance, bilingual clarity, knowledge accuracy and escalation wording. Define thresholds **before** evaluation; no fabricated medical success rate.

## Data/privacy gates
1. Completely offline runtime; do not declare Android INTERNET permission in the inference-only flavor; audit dependencies and native code, avoid runtime download.
2. No audio/health logs; only opt-in history persists locally; Android Keystore-backed authenticated encryption, protected key rotation, tested deletion and no cloud backup of sensitive files.
3. Signed/versioned manifest includes artifact hash, byte size, owner, specific license, source/revision, quantization, tokenizer and compatible ABI/runtime. Reject mismatched assets without trying unsafe fallback versions.
4. Local speech and KB resources may contain copyrighted works; evaluate component and weight licenses separately from MIT/Apache runtimes.
5. Offline airplane-mode test must succeed on actual supported phones after necessary assets are present.

## Experiments to close Phase 00 (not executed here)
**E01** Sanitized hardware inventory and ABI/RAM/free storage via adb.
**E02** Test exact Gemma 3 1B QAT .litertlm + pinned Kotlin AAR on target handset: CPU successful load, prompt/response, 2048 effective context, cancel, memory and speed; GPU optional.
**E03** Test whisper.cpp multilingual base/tiny on a small consented balanced Telugu/English/code-switch health audio set; record WER/CER/entity errors and Android RTF/PSS.
**E04** Exact E5 ONNX + tokenizer identical to Python desktop reference; run 384-d embeddings on Android with fixed inputs; quantify vector parity and bilingual retrieval quality.
**E05** Validate offline EN **and TE** TTS by voice enumeration + airplane-mode synthesis. If te missing, identify a license-compliant local model and demonstrate load/playback; otherwise mark product path blocked.
**E06** Sequential resource stress, restart/cancel/repeat tests with no OOM; GPU/NPU only if supported. Verify no outgoing internet and SHA256 correctness.
**E07** Knowledge-source rights register, source format manifest and preliminary clinician-reviewed bilingual evaluation specification (not certification).

## Phase decision rubric
- **Documentation prepared:** yes.
- **Architecture appears implementable at subsystem level:** yes, with exceptions noted.
- **Comprehensive mobile compatibility proven:** **no**.
- **Complete Telugu voice available and rights validated:** **no**.
- **Medical safety/quality validated:** **no**.
- **Phase status:** **AWAITING VERIFICATION**. Phase 01 *planning/scaffolding* can be proposed for review, but no claim of Phase 00 technical completion or complete product feasibility yet.

## Linked records
- [Model selection](architecture/model-selection.md)
- [Device requirements](architecture/device-requirements.md)
- [Technology stack](architecture/technology-stack.md)
- [Phase log](progress/phase-00.md)

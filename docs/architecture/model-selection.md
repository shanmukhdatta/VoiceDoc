# Phase 00 — Model selection and compatibility matrix
Date: 2026-10-13 · Scope: English (en) and Telugu (te) ONLY
Status: **Provisional; device validation and language-quality validation outstanding.**
Source repository: https://github.com/shanmukhdatta/VoiceDoc

## Interpretation
- **DOC VERIFIED** = official model/runtime documentation establishes the capability, not tested here.
- **CANDIDATE** = technically plausible, but a target-device or conversion test is missing.
- **BLOCKER** = no verified complete, license-compatible path for a mandatory release feature.
- **TESTED** = executed with retained logs on a named environment. **No model is TESTED in Phase 00.**

| Subsystem | Candidate | Language / format | Android inference route | License state | Assessment / gate |
|---|---|---|---|---|---|
| VAD | Silero VAD v6.x | speech activity; ONNX | ONNX Runtime Android CPU; preserve recurrent state | MIT (source/model) | DOC VERIFIED runtime; device test pending |
| ASR default | whisper.cpp multilingual **base** | te/en; GGML, 142 MiB unquantized | official whisper.android + NDK/JNI CPU | MIT implementation; verify original Whisper weights provenance | DOC VERIFIED implementation; medical Telugu accuracy unknown |
| ASR lower memory | whisper.cpp multilingual **tiny** | te/en; GGML, 75 MiB | same route; optional quantization | same | CANDIDATE for low-memory tier; accuracy may be inadequate |
| ASR research fallback | AI4Bharat IndicConformer | Telugu; 600M multilingual released under MIT | ONNX model exists, but Android decoding/custom ops not demonstrated by upstream | MIT indicated by upstream checkpoint | NOT a drop-in: high footprint and export/CTC or RNNT decoder integration unresolved |
| Embedder | intfloat/multilingual-e5-small | 384-d vectors; ONNX assets provided; query/passages prefixed | ONNX Runtime Android CPU + Kotlin tokenization | MIT | DOC VERIFIED checkpoint and general runtime; **Android tokenizer, graph and Telugu retrieval untested** |
| LLM | litert-community/Gemma3-1B-IT QAT int4 | text, .litertlm choice; Gemma 3 1B max 32K theoretical context | LiteRT-LM Kotlin API (CPU/GPU) | **Gemma Terms**, not Apache/MIT; license acceptance/redistribution review | DOC VERIFIED reference Android execution, **our artifact/device untested** |
| LLM optional accelerator | Gemma3-1B-IT NPU variant | QNN-ready/A16W4 where provided | LiteRT-LM NPU and compatible Snapdragon + libraries | same Gemma terms, QNN distribution obligations | conditional; **NOT assumed available** |
| TTS en primary | installed Android TextToSpeech engine | en-US offline voice | Android platform API; verify Voice.isNetworkConnectionRequired()==false and airplane mode | vendor voice terms; no redistribution if OS-provided | device-specific; not guaranteed |
| TTS en fallback | sherpa-onnx + licensed Piper/VITS English | ONNX | sherpa-onnx Android JNI | engine permissive; **individual voice licensing must be audited** | upstream Android path exists; model-dependent |
| TTS te primary | installed Android TextToSpeech engine | te-IN offline voice, if installed | Android API + airplane-mode test | vendor TTS license | **BLOCKER until an actual offline voice is verified on target phone** |
| TTS te fallback research | compact Telugu VITS/other with lawful redistribution | Telugu ONNX + matching tokenizer/phonemizer | sherpa-onnx Android **only after runtime compatibility proof** | TBD | **BLOCKER**: no selected verified commercial-compatible mobile-ready Telugu voice |
| TTS te research only | AI4Bharat IndicF5 | te speech, MIT repository/model listing | upstream requires PyTorch + prompt audio/reference; no verified mobile path | MIT model; audit all training/reference audio and dependencies | possible later conversion, **not release candidate** |
| TTS te excluded from future commercial pack | Meta MMS-TTS Telugu | Telugu | PyTorch/VITS may be exportable | **CC BY-NC 4.0** | **EXCLUDED** from commercial redistribution |
| Retrieval | Room/SQLite FTS5 + exact cosine | English/Telugu unicode data, vector BLOB/files | standard Android SQLite plus bundled exact float vector search | framework Apache-style; SQLite public domain; audit dependencies | engineering design pending benchmarking |
| Safety | deterministic bilingual intent/risk rules + structured output/evidence validator | te/en | Kotlin (no model dependence) | authored code + reviewed content | **design only**; clinical evaluation is essential |

## LLM boundaries
- Start from the upstream **quantized LiteRT-LM-compatible artifact**, not any arbitrary .gguf/.safetensors renamed to .litertlm. Verify artifact SHA256, actual extension, runtime version and on-device load before selecting it.
- Google's published LiteRT-LM Gemma 3 1B **QAT INT4** Android table gives a **529 MB** serialized model and roughly **1009 MB CPU RSS** or **1205 MB CPU RSS on GPU configuration**, measured on **Samsung S24 Ultra** at 2048 context. This is a third-party upstream measurement, **not a VoiceDoc measurement**. NPU table on S25 Ultra is also not transferable to another handset.
- The base Gemma 3 1B model card states **32K max model context**, not 128K (which is for larger sizes); deployed mobile artifact context can be further restricted. Propose experimental effective input+output budget ~2048 tokens, not a guaranteed model limit.
- Document system prompts, tokenizer, streaming callback contract, error recovery and cancellation behind a `LanguageModelEngine`; do not assume all backends handle tool calls or structured JSON equally.
- CPU fallback is a *required acceptance gate*, GPU and NPU optional optimizations. LiteRT-LM GPU on Android may require libOpenCL.so and libvndksupport.so declarations; NPU requires compatible SoC, model compilation and native SDK.

## Speech / code-switching
- Use *multilingual* base/tiny checkpoints, NOT base.en/tiny.en (English-only).
- Preserve audio as 16 kHz mono PCM, segment with VAD and bound maximum segment duration. Keep original transcript and conservative normalization; never silently convert units, medicine names or negations.
- Evaluate separately Telugu, English, code-switched and accented speech, with a dedicated medical term/entity error rate and numeric/unit fidelity. Whisper multilingual existence **does not prove sufficient Telugu health ASR**.
- IndicConformer-600M multilingual is ~600M params and uses a custom model interface; don't count upstream ONNX listing as a finished APK implementation. Consider a smaller Telugu-specific checkpoint only if exact license, size, tokenizer, decoder and Android export pass evaluation.

## Embeddings / RAG
- E5-small upstream has 12 layers, 384 features, MIT license and repository ONNX export. The listed unquantized PyTorch weights are approximately 471 MB; the delivered optimized encoder **must be measured** rather than assuming it is already small.
- Reproduce the E5 prefix convention: `query: ...` and `passage: ...`; tokenize identically offline, apply appropriate masked mean pooling, L2-normalize vectors, and compare against a Python golden reference.
- Baseline lexical via SQLite FTS5 and dense cosine retrieval, reciprocal rank fusion or calibrated weighted combination after evaluation; never normalize Telugu by translating it silently.
- For 5,000 chunks x 384 x 4 bytes, raw FP32 vectors ~7.32 MiB (decimal ~7.68 MB), **before metadata, SQLite/page/index overhead**. At 10,000 chunks ~14.65 MiB. Use precomputed document vectors and exact search until benchmark warrants ANN.
- Keep source_id / chunk_id / title / URL / publisher / date / language / content hash / license / review version; no untraceable snippets.

## TTS release gate (critical)
Android TextToSpeech support is determined by installed engine, installed voices and whether each voice requires network. `isLanguageAvailable(te-IN)` alone is insufficient. Enumerate `tts.voices`, locate Telugu `Locale("te","IN")`, reject `isNetworkConnectionRequired`, synthesize and listen **while offline**. A network-free English voice on one phone does not imply Telugu works.
If offline Telugu is absent, retain text-only experimental mode **but block claiming complete product readiness**. Candidate sherpa-onnx VITS needs a legitimately licensed pretrained Telugu ONNX package, phonemizer/tokenizer mapping, memory/latency measurement and human pronunciation checks. IndicF5 is not presumed convertible: investigate actual graph export and prompt-audio needs.

## Stable upstream references (accessed 2026-10-13)
- LiteRT-LM Android Kotlin API: https://developers.google.com/edge/litert-lm/android
- LiteRT community Gemma 3 quantized deployment and S24/S25 performance tables: https://huggingface.co/litert-community/Gemma3-1B-IT
- Gemma 3 1B card and terms: https://huggingface.co/google/gemma-3-1b-it
- QNN supported chipsets: https://developers.google.com/edge/litert/next/qualcomm
- whisper.cpp Android, quantization, RAM: https://github.com/ggml-org/whisper.cpp
- whisper.cpp model sizes: https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md
- Silero VAD: https://github.com/snakers4/silero-vad
- IndicConformer 600M checkpoint: https://huggingface.co/ai4bharat/indic-conformer-600m-multilingual
- Multilingual E5 small weights and ONNX listing: https://huggingface.co/intfloat/multilingual-e5-small
- ONNX Runtime Android: https://onnxruntime.ai/docs/build/android.html
- sherpa-onnx TTS model API: https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/c-api/docs/tts.dox
- Android TextToSpeech Voice API: https://developer.android.com/reference/android/speech/tts/Voice
- IndicF5: https://github.com/AI4Bharat/IndicF5 and https://huggingface.co/ai4bharat/IndicF5
- MMS Telugu licensing (noncommercial): https://huggingface.co/facebook/mms-tts-tel

## Selection policy
Freeze a specific checkpoint revision, binary SHA256, tokenizer revision, license text, runtime ABI, output shape, supported languages, and measurement source in later model manifests. **No model binaries are committed in Phase 00.** No model is selected permanently until physical-device and bilingual medical-quality gates pass.

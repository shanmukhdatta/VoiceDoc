# Voice Doctor AI — Full 10-Day Hands-On Implementation Plan

> **Scope:** The *entire* 40-section [VoiceDoc README](README.md) — Android, offline voice ASR, English/Telugu/Tamil language processing, local medical RAG, quantized on-device SLM, evidence-aware medical-safety pipeline, offline TTS, fine-tuning/quantization research, mobile acceleration, privacy, testing, benchmarking and release engineering.
>
> **Method:** **LEARN → BUILD A TINY EXERCISE → BUILD THE REAL COMPONENT → EXPERIMENT → VERIFY → EXPLAIN → COMMIT.**
>
> **Status:** This is a planning/checklist document, **not proof that any implementation is complete**. Leave checkboxes blank until verified. No GitHub code changes or releases are implied by marking this plan available.

## Start here: how to follow these files

1. Open **Day 01** below; finish its **Task A** learning checklist before trying **Task D** implementation.
2. For each subtask, read the linked docs, write a minimal program or experiment, implement it in VoiceDoc, run the listed tests, and **only then** tick `- [x]`.
3. Open this Markdown in GitHub, click the pencil icon, replace the checkbox's space with `x`, commit the documentation change, or edit locally with your IDE and `git add/commit/push`.
4. Do not check whole days complete from successful compilation alone; the **exit criteria and actual device evidence** are mandatory.
5. Record commands, output, component explanation, model/version/license, device details, screenshots and failures in `docs/learning/day-XX-notes.md` while implementing.
6. Use AI to ask *why*, request one hint, compare designs, review your code and interpret test results. **Attempt to write your own solution first.**
7. If blocked by GPU, TTS language voice, runtime operator, device memory or policy review: write `BLOCKED` and exact reason, continue only independent tasks. A fake adapter is permitted for UI tests, **not** for claiming a real prototype.
8. Never share patient data, API secrets, keystores or sensitive recordings in public repositories. The app is a research health-*information* tool, **not** a diagnosis/treatment system.

## Non-negotiable reality check

- A ten-day intensive sprint is an **engineering target**, not a guarantee of fully verified medical correctness, successful fine-tuning, NPU compilation, all-language TTS coverage, or approval by an app store.
- **The intended release is an installable Android offline research prototype** with measured limitations; publicly distributable model assets need proper licenses and privacy/safety review.
- The baseline GitHub repository already has a Kotlin/Compose Android app, English/Telugu foundation UI, `:app` and `:core:common`. **Continue from this code; do not delete and rebuild blindly.**
- `RAG` here means **retrieval-augmented generation**, not the React JavaScript UI framework. The mobile app uses Kotlin/Jetpack Compose, with offline Python for dataset/index/training tools.
- Real-time cloud APIs or a hosted backend are **not required for inference**. Model provisioning can be a separate optional action while connected, then all inference must pass airplane-mode tests.
- Scope includes **English, Telugu and Tamil** from README; each language has separate acceptance metrics. An unvalidated language or missing offline TTS voice remains explicitly limited.
- If the physical test phone cannot load the selected model, choose a compatible model/runtime and record the deviation before claiming success.

## Project execution board — tick ONLY when exit criteria pass

| Day | Date (IST) | Detailed subtasks | The measurable milestone | Status |
|---|---|---|---|---|
| 01 | Oct 26 | [Day 01](docs/implementation/DAY_01.md) | Compiling app architecture, navigation, interfaces | ⬜ |
| 02 | Oct 27 | [Day 02](docs/implementation/DAY_02.md) | Chat state, local settings, real PCM microphone capture | ⬜ |
| 03 | Oct 28 | [Day 03](docs/implementation/DAY_03.md) | Licensed medical corpus with deterministic manifests | ⬜ |
| 04 | Oct 29 | [Day 04](docs/implementation/DAY_04.md) | Real offline multilingual embedding retrieval | ⬜ |
| 05 | Oct 30 | [Day 05](docs/implementation/DAY_05.md) | Real quantized SLM answering via offline RAG | ⬜ |
| 06 | Oct 31 | [Day 06](docs/implementation/DAY_06.md) | End-to-end safety-gated *typed* answer pipeline | ⬜ |
| 07 | Nov 01 | [Day 07](docs/implementation/DAY_07.md) | Local VAD + ASR and editable transcript | ⬜ |
| 08 | Nov 02 | [Day 08](docs/implementation/DAY_08.md) | Verified multilingual voice/text path and offline TTS | ⬜ |
| 09 | Nov 03 | [Day 09](docs/implementation/DAY_09.md) | Reproducible training/quantization/hardware benchmark pipeline | ⬜ |
| 10 | Nov 04 | [Day 10](docs/implementation/DAY_10.md) | Model manager, metrics, offline acceptance, signed release candidate | ⬜ |

### Master checkbox tracker (one box per day)

- [ ] **Day 01** — [Kotlin, Compose, app architecture, navigation, first tests](docs/implementation/DAY_01.md)
- [ ] **Day 02** — [Interactive chat, state, Room/DataStore, audio recording](docs/implementation/DAY_02.md)
- [ ] **Day 03** — [Licensed, trusted knowledge ingestion/chunking/provenance](docs/implementation/DAY_03.md)
- [ ] **Day 04** — [Embeddings, on-device vectors, retrieval and metric checks](docs/implementation/DAY_04.md)
- [ ] **Day 05** — [LiteRT-LM, real quantized SLM, streaming RAG generation](docs/implementation/DAY_05.md)
- [ ] **Day 06** — [Safety engine, prompt guardrails, privacy and orchestration](docs/implementation/DAY_06.md)
- [ ] **Day 07** — [VAD, whisper.cpp, native bridge and speech evaluation](docs/implementation/DAY_07.md)
- [ ] **Day 08** — [Telugu/Tamil/English processing, offline TTS and full pipeline](docs/implementation/DAY_08.md)
- [ ] **Day 09** — [LoRA/QLoRA, export, quantization and CPU/GPU/QNN checks](docs/implementation/DAY_09.md)
- [ ] **Day 10** — [Packaging, benchmark dashboard, CI, airplane-mode test and signed APK](docs/implementation/DAY_10.md)

## Suggested full-day learning rhythm

| Block | Work | Proof |
|---|---|---|
| 1 — ~1.5 hours | Official tutorials and concept notes | Explain architecture in your own words |
| 2 — ~1.5 hours | Small isolated programming exercise | One working minimal example and tests |
| 3 — ~3 hours | VoiceDoc real component implementation | Source changes + compile + integration tests |
| 4 — ~2 hours | Experiments and negative/error tests | Benchmark table and command logs |
| 5 — ~1 hour | Fix failures, code review, explain-and-commit | Commit SHA, progress, honest blockers |
| Extra time | Complex integrations, large model downloads, GPU/NDK/debugging | Continue next day without pretending completion |

**Dependency gates beat the calendar.** You can work on independent preparation if blocked, but never skip the prerequisite for a downstream claim. Medical safety, privacy, license checks and grounded citations cannot be left as TODOs in a publicly presented health prototype.

## Full system pipeline (integration contract)

```text
MICROPHONE / USER TEXT
  ↓
AudioRecord → Voice activity detection → Offline ASR  [speech only]
  ↓
Transcript correction → Language select/normalize
  ↓
INPUT SAFETY / INTENT / URGENCY SCREEN
  ↓
Multilingual query embedding → Local vector search
  ↓
Trusted knowledge chunks + source metadata → Context budget
  ↓
Policy-aware prompt + limited opt-in session context
  ↓
Quantized on-device SLM (LiteRT-LM, CPU/GPU and supported optional NPU)
  ↓
OUTPUT SAFETY + GROUNDING/CITATION VALIDATION
  ↓
Safe text + source references → Verified offline TTS
  ↓
Local performance metrics (no raw private health text)
```

**Separate build-time workflows:** trusted knowledge ingestion/indexing (`rag/`, `knowledge/`) and model training/quantization/conversion (`ml/`, `models/`) produce versioned assets verified by the on-device model manager.

## Definition of done — final physical-device prototype

- [ ] App is installable on a supported physical Android phone and survives cold start/restart.
- [ ] Real offline microphone input works; VAD and on-device ASR produce an editable transcript.
- [ ] Typed input follows the *same* RAG+SLM pipeline as spoken input.
- [ ] Source-attributed medical knowledge is locally stored with provenance and reuse permission.
- [ ] Real query embeddings and on-device top-K retrieval run without Internet.
- [ ] A real quantized on-device SLM generates grounded content; no disguised API or mock.
- [ ] The input and output safety gates, uncertainty handling and urgency policy are exercised by labelled tests.
- [ ] Only approved output is displayed/spoken; citations map to actual retrieved source IDs.
- [ ] Verified language support is shown separately for en/te/ta across ASR, retrieval, generation and TTS.
- [ ] Full voice → knowledge → answer → offline voice demo succeeds on at least one verified language path.
- [ ] Optional history is off by default; private data stays local and can be deleted.
- [ ] Model packs are versioned, checksum-verified and compatible with device/runtime.
- [ ] Developer dashboard reports **measured**, not illustrative, latency, TTFT, tokens/s, memory and backend.
- [ ] Tests cover no microphone, missing/corrupt model, missing index, no matching sources, out-of-scope input, cancellation and OOM where testable.
- [ ] APK is signed and installed/tested; release documentation states device specs and known limitations.
- [ ] Code, models, datasets, medical content, TTS voices and icons/assets have compatible redistributable terms.
- [ ] README includes architecture, setup, how to obtain models, verification commands, demo, supported languages and limits.
- [ ] If a GitHub Release is published, APK and checksums match the final verified build.
- [ ] Any Google Play distribution is separate: developer eligibility, policy/privacy review and store approval are confirmed before claiming it is available.
- [ ] No diagnosis, clinical accuracy or emergency reliability claim is made without suitable clinical review and validation.

## Full README scope traceability — every major section accounted for

| README section | Where implemented / verified |
|---|---|
| 1 Vision, 2 product boundary, 3 flow, 4 architecture | Day 01 design; Days 06/10 requirements check |
| 5 recommended technology stack | Day 01 setup; pinned dependencies throughout |
| 6 Speech-to-text | Days 02, 07, 08 |
| 7 Language-processing layer | Days 01, 07, 08 |
| 8 Offline RAG architecture | Days 03–05 |
| 9 Embeddings engine | Day 04 |
| 10 Local vector store | Day 04 |
| 11 Prompt construction | Days 05–06 |
| 12 On-device SLM | Day 05 |
| 13 Attention/context efficiency | Days 05, 09 |
| 14 LiteRT-LM runtime | Days 05, 09 |
| 15 Qualcomm acceleration | Days 09–10 (supported hardware only) |
| 16 Domain adaptation/fine-tuning | Day 09 |
| 17 Quantization/optimization | Days 05, 09 |
| 18 Output safety | Day 06; regression Day 10 |
| 19 Offline text-to-speech | Day 08 |
| 20 Runtime sequence | Days 06–08, full integration Day 10 |
| 21 Android module architecture | Day 01; maintained Days 02–10 |
| 22 Repository structure | Day 01; folders added per component |
| 23 Model asset management | Days 05, 09–10 |
| 24 Conversation memory | Days 02, 06 |
| 25 Privacy architecture | Days 02, 06, 10 |
| 26 Medical knowledge governance | Day 03; audit Day 10 |
| 27 Evaluation architecture | Days 03–10; consolidated Day 10 |
| 28 Developer performance dashboard | Days 05, 07, 09, 10 |
| 29 Airplane-mode acceptance | Days 05–08; full test Day 10 |
| 30 Error/fallback architecture | Days 02, 04–08, 10 |
| 31 Core interfaces | Day 01; implemented over Days 02–10 |
| 32 Orchestrator | Day 06; integration Days 07–10 |
| 33 Principles, 34 technical differentiators | Day 01; retrospection Day 10 |
| 35 Demo story | Day 10 |
| 36 Technology matrix, 37 references | Day 01 selection; version audit Day 10 |
| 38 Success criteria, 39 status, 40 safety notice | This tracker + Day 10 release gates |

## Expected repository ownership

```text
VoiceDoc/
├── README.md
├── IMPLEMENTATION_PLAN.md            ← THIS MASTER CHECKLIST
├── docs/
│   ├── implementation/
│   │   ├── DAY_01.md ... DAY_10.md   ← FOLLOW ONE PER DAY
│   ├── learning/
│   │   └── day-XX-notes.md            ← CREATE AS YOU BUILD
│   └── architecture/, evaluation/, safety-card/, model-card/
├── android/
│   ├── app/, feature/chat/
│   ├── core/common/, core/audio/, core/asr/, core/language/
│   ├── core/embedding/, core/rag/, core/llm/, core/safety/
│   ├── core/tts/, core/data/, core/model-manager/, core/metrics/
│   └── benchmark/
├── rag/ ingestion/, cleaning/, chunking/, embedding/, indexing/, retrieval_eval/
├── knowledge/ source_manifests/, licenses/
├── ml/ datasets/, preprocessing/, training/, evaluation/, quantization/, conversion/, export/, notebooks/
├── safety/ policies/, rules/, test_cases/, evaluation/
├── models/ manifests/, checksums/
├── tests/ golden/, multilingual/, medical/, offline/
└── benchmarks/ devices/, asr/, embeddings/, retrieval/, llm/, tts/, end_to_end/
```

**The directory tree above is the *target*, not a claim that all folders already exist.** Create real modules/folders only when their implementation begins.

## Source of truth and evidence log

At the end of each day fill this table yourself:

| Day | Commit SHA | Tests run and results | Device/model | Measured result | Known blocker |
|---|---|---|---|---|---|
| 01 | — | Not run | — | — | — |
| 02 | — | Not run | — | — | — |
| 03 | — | Not run | — | — | — |
| 04 | — | Not run | — | — | — |
| 05 | — | Not run | — | — | — |
| 06 | — | Not run | — | — | — |
| 07 | — | Not run | — | — | — |
| 08 | — | Not run | — | — | — |
| 09 | — | Not run | — | — | — |
| 10 | — | Not run | — | — | — |

### Final publication decision

- [ ] **Internal demonstration:** all necessary offline acceptance criteria are evidenced for the chosen device.
- [ ] **Research distribution / GitHub Release:** signing, redistribution licenses, privacy and limitations are checked.
- [ ] **Public Play Store listing (optional):** health-related policies, legal/age requirements, developer account and store review are addressed.
- [ ] **Clinical claims:** remain strictly limited until independently evaluated with suitable professional involvement.

**Never trade trustworthy behavior for a demo deadline.**

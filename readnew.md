# VoiceDoc Research Experiments — English–Telugu Medical RAG
## Independent research README and executable checklist

**Repository:** https://github.com/shanmukhdatta/VoiceDoc  
**Research kickoff:** 2026-10-31  
**Document status:** Research plan only; no experiments, datasets, model integrations, or clinical validation are implied by this file.  
**Original app docs:** [README.md](README.md) · [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) · [10-day day-by-day files](docs/implementation/DAY_01.md)

> **Document rule:** This file is ADDITIVE. It does not replace or override the original product README, app implementation plan, or any existing code. It defines a parallel, reproducible research track. Never check a box until its evidence is committed or recorded. **Only English and Telugu are research targets**, including native-script Telugu, Romanized Telugu, and English–Telugu code mixing. Other README languages are outside the current experiments, not silently deleted from the original product vision.

---

## 0. What are we trying to prove?

**Working research problem:** Can a resource-efficient English–Telugu medical-information retrieval system preserve important *clinical constraints* across scripts and code mixing so that it can distinguish evidence that merely discusses the topic from evidence that is sufficient to support a limited informational response?

**Candidate method name:** CS-CLINSUFF (provisional; do not claim it as a validated or original algorithm until peer work and controlled experiments justify that claim).

**Research focus (in priority order):**
1. **Cross-script constraint preservation**: entity, negation, population/context, quantity, duration, and relationships (e.g. "not safe for group X" versus "safe for group X").
2. **Pre-generation evidence sufficiency**: support / missing evidence / contradictory evidence on the *retrieved set*, not just high document similarity.
3. **Retrieval robustness**: compare English, తెలుగు, Romanized Telugu (Tenglish), and mixed-language formulations.
4. **Efficiency and early exit**: only AFTER valid evidence decisions are established, test whether avoiding unnecessary generation saves time on actual phone hardware.

**This is NOT a diagnostic or medical-advice system.** Use general health-information questions and traceable public sources. Do not use patient-identifiable data, provide definitive treatment recommendations, or claim clinical reliability absent qualified review.

### Research questions and falsifiable hypotheses

- **RQ1 (retrieval):** How does lexical, dense, and hybrid retrieval performance change across four English–Telugu script/form variants?
- **RQ2 (sufficiency):** Can explicit constraint checks distinguish missing, sufficient, and contradictory evidence more accurately than dense-score or lexical-coverage thresholds on held-out, independently reviewed cases?
- **RQ3 (robustness):** Are gains preserved across negation, numbers/units, age/pregnancy modifiers, colloquial forms, and unseen terminology?
- **RQ4 (efficiency, conditional):** If the gate is reliable, how much generation time is avoided by abstaining *before* the SLM runs, and at what cost to answer coverage and quality?

**Do not pre-announce effect sizes.** The 67-page Deep Research report's percentage improvements, sub-350-ms mobile claim, and memory estimates are unmeasured hypotheses. Report what actually happens, including failures and negative results.

### What is not novel by itself

- Original plus transliterated/translated query retrieval and RRF fusion.
- Code-switch-aware query routing generally.
- Three-way evidence labels or pre-generation refusal generally.
- Combining off-the-shelf ASR, RAG, SLM, TTS, and Android.
- A symbol/lexicon matcher with no controlled improvement over simpler baselines.

**Novelty bar:** Demonstrate a specific gap in *English–Telugu clinical constraint relation preservation and evidence adequacy*, with strong close-prior comparisons and independent evaluation. The working mechanism can change if results disprove the hypothesis.

---

## 1. Current repository reality and how tracks connect

At research kickoff, default-branch inspection showed an Android Kotlin/Compose foundation under:
- android/app/src/main/java/ai/voicedoc/app/MainActivity.kt
- android/core/common/src/main/java/ai/voicedoc/core/common/SupportedLanguage.kt
- android/app/build.gradle.kts, android/settings.gradle.kts
- android/app/src/androidTest/, android/core/common/src/test/
- .github/workflows/android-foundation.yml

The full RAG, embeddings, ASR, quantized LLM, and safety research pipeline is **not established as implemented by merely appearing in the 40-section product README**. Audit before coding and recheck the branch when starting each phase. The original [implementation plan](IMPLEMENTATION_PLAN.md) continues independently. Its inclusion of a broader future language scope must not be mistaken for results in Telugu and English.

**Parallel workstreams:**
- **Research (this file):** licensed evidence, controlled benchmark, baselines, constraint system, evaluation, reproducibility, experiments.
- **Android product (original plan):** native voice UI, offline ASR/RAG/SLM/TTS, privacy, local model-pack delivery, safety integration, real-device benchmarks.

Do **not** block Python retrieval and sufficiency experiments on completing a release APK. Do **not** claim real Android speed based on Kaggle experiments.

---

## 2. Primary literature and comparison obligations

Read and verify the PRIMARY sources, not only the 67-page generated report:

| Closest work / precedent | Link | Why we must compare |
|---|---|---|
| SETU-RAG (2026), code-switching-aware multilingual RAG | https://digitalcommons.isical.ac.in/masters-dissertations/458/ | Indic transliteration, routing, retrieval fusion; **check the actual adaptive router** rather than assuming unconditional fan-out |
| SURE-RAG (2026), evidence sufficiency | https://arxiv.org/abs/2605.03534 | Support, refutation, insufficiency and risk–coverage decisions |
| Requirement-directed / hypergraph selective retrieval (2026) | https://arxiv.org/abs/2610.34684 | Closest competitor for explicit query requirements and evidence linking |
| Evidence-sufficiency boundary learning (2026) | https://arxiv.org/abs/2609.01687 | Evaluating transitions from partial to adequate context |
| MiniCheck (EMNLP 2024) | https://aclanthology.org/2024.emnlp-main.499/ | Fact-checking and claim–document entailment |
| Self-RAG (ICLR 2024) | https://arxiv.org/abs/2310.11511 | Retrieval/generation/critique; not a new general idea |
| Corrective RAG (2024) | https://arxiv.org/abs/2401.15884 | Retrieval evaluator and routing precedent |
| BGE-M3 (2024) | https://arxiv.org/abs/2402.03216 | Multilingual dense/sparse/multi-vector retrieval backbone |

**Citation-review checklist**
- [ ] Read methods and evaluation sections of SETU-RAG; write exact overlap and differences.
- [ ] Read SURE-RAG; distinguish pre-generation evidence assessment from checking a generated answer.
- [ ] Read requirement-directed/hypergraph selective retrieval; compare requirement extraction and set-level checking.
- [ ] Verify that claimed model architectures, public checkpoints, licenses, and metrics actually match the linked original sources.
- [ ] Build a closest-work table with limitations and which experiment truly discriminates our method.
- [ ] Mark each novelty claim as **SUPPORTED / UNCERTAIN / ALREADY KNOWN** before including it in a manuscript.

---

## 3. Data and ethics: source-verification gate (must complete first)

**Never presume public web access implies redistribution rights.** Store source URL, publisher, date accessed, document section, copyright/licensing terms, transformations, and hash before indexing. Verify each exact artifact's terms.

Candidates to investigate (not approved automatically):
- **Eka-IndicMTEB / IndiaAI AIKosh** — bilingual medical terminology/embedding benchmark; useful for term alignment but **not a gold document-level sufficiency benchmark**. Verify the exact downloadable artifact and applicable license.
- **Vikaspedia English/Telugu health pages** — possible public reading/source material; **reproduction and redistribution require rights checks and permission where applicable**. Do not publish scraped raw text or bundle it in the APK by default.
- **IndicMedDialog** — multilingual synthetic medical conversations; confirm specific dataset license and distinguish synthetic text from real consultations.
- **Government/public-health information pages and other medical sources** — confirm publisher, medical authority, content age, stable link, permission to reuse, and availability of corresponding Telugu material.
- **SNOMED CT / Global Patient Set** — licensing conditions must be reviewed; free availability is not a blanket right to redistribute derivative vocabulary packs or unrestricted terminology.
- **MedHallu, MILU, PubMedQA** — possible auxiliary evaluations, but examine task mismatch and licenses before use; exam QA or synthetic hallucination labels are not automatically clinical sufficiency gold labels.

**Required source manifest schema:** source_id, title, publisher, canonical_url, publication_date, accessed_at, document_version, license_text_or_url, reuse_status (APPROVED / INTERNAL_ONLY / BLOCKED / UNKNOWN), redistribution_allowed (true/false/unknown), language, transformation, sha256, reviewer, notes.

**Checklist (Phase D0):**
- [ ] Select at least one source with clearly documented permissions for **the intended research use**.
- [ ] Record license and redistribution status for each source; remove or block unknown-license items from distributable assets.
- [ ] Capture document-level provenance and timestamps; deduplicate by content hash and near-duplicate signatures.
- [ ] Define a controlled medical-information scope (e.g., a small set of non-diagnostic public-health education topics).
- [ ] Identify bilingual Telugu/English reviewer availability; clinical correctness claims require suitable expert review.
- [ ] Freeze source IDs and source text versions; write a manifest.
- [ ] **Gate D0:** Stop benchmark creation if sources lack traceable evidence and usage rights.

**Deliverables to implement later:** research/voicedoc/data/sources_manifest.csv, research/voicedoc/docs/DATA_GOVERNANCE.md, knowledge/source_manifests/ (where suitable). These paths are proposed, NOT present yet.

---

## 4. TeMed-Suff pilot benchmark (scientifically bounded)

**Phase D1 goal:** Build **20–30 source-grounded question families** for a pilot. Do not describe them as clinically validated unless reviewed. Proposed full-scale study may expand to 150 families only after the pilot passes quality checks.

A **family** means one underlying question and evidence requirement. Each family can have:
1. **EN:** English.
2. **TE:** Telugu native script.
3. **ROM:** Romanized Telugu.
4. **CM:** English–Telugu code-mixed formulation.

Evidence conditions (keep separately labelled and independently checked):
- **C0 irrelevant**: unrelated evidence.
- **C1 partial**: correct topic, required qualifier missing.
- **C2 sufficient**: source contains all relevant facts required for the narrowly defined informational answer.
- **C3a sufficient + distracting**: adds irrelevant or partially relevant passages without invalidating support.
- **C3b contradictory**: competing passages disagree on a relevant condition; explicitly label synthetic corruption.

**Planning example:** 24 families × 4 formulations = 96 open-retrieval queries; 24 × 4 × 5 = 480 controlled query–context observations. These are **design counts, not created data**. Group every variant and context of a family into the same split, and group by underlying source as needed. Do not use the same fact pair with cosmetic paraphrasing across train and test.

**Annotation fields:**
family_id, question_id, split, script_form, canonical_query_intent, original_question, provenance_ids, evidence_doc_ids, gold_relevance, gold_sufficiency (SUPPORTED / INSUFFICIENT / CONFLICTING), explicit_required_entities, required_relations, negation_scope, population/age, numbers/units, duration, safe_information_scope, synthetic_perturbation_flag, annotator_id, adjudication_status, license_status.

**Benchmark checklist:**
- [ ] Freeze family concepts BEFORE selecting experimental examples.
- [ ] Author each query in idiomatic English/Telugu; check Telugu wording with a competent speaker, not just blind machine translation.
- [ ] Create realistic Romanized Telugu variants and code-switch forms; preserve semantic equivalence.
- [ ] Identify required fact relationships, not just entity names.
- [ ] Independently verify C2 support against source documents.
- [ ] Check that C1 *actually removes an essential condition* and C3b creates a real contradiction on the **same** population/time/scope.
- [ ] Avoid language-form shortcuts, fixed synthetic templates, or metadata leakage revealing the gold class.
- [ ] Review C0–C3b labels independently of the rule implementation whenever possible.
- [ ] Deduplicate within and across families; split **by family and source** before threshold tuning.
- [ ] Reserve held-out test families. If the pilot is too small for meaningful statistical inference, report it as a **pilot** and collect more independently reviewed families.
- [ ] Audit annotator disagreements, ambiguous conditions, and examples that should be excluded.
- [ ] **Gate D1:** Dataset version + checksums + provenance + sample count + annotation-quality notes exist, with NO synthetic example presented as real clinical evidence.

**Proposed artifacts:** research/voicedoc/data/benchmark/schema.json, families.jsonl, evidence_contexts.jsonl, splits.json, annotation_guide.md, review_log.csv, dataset_card.md. Keep copyrighted source text out of a public repository unless permission permits.

---

## 5. Phase B0 — Establish honest baseline retrieval

Run experiments on the SAME frozen candidate corpus, query families, and evaluation protocol. Measure separately for EN/TE/ROM/CM.

**Mandatory baselines**
- **B1** BM25 lexical retrieval (report tokenization and Telugu morphology limitations).
- **B2** Multilingual dense embedding retrieval, exact checkpoint and embedding preprocessing recorded.
- **B3** Hybrid lexical+dense retrieval with validation-only normalization and fusion tuning.
- **B4** Canonicalization/transliteration or translation-then-retrieve baseline, **only if exact model and code path are verified**.
- **B5** Fixed dual-view retrieval plus RRF; compare against established SETU-style routing correctly, and do not call RRF novel.

**Optional baselines:** stronger reranker, established requirement-based selective retriever, multilingual entailment verifier, prompt-only abstention. Label unavailable ones and explain why; do not silently substitute a weaker implementation and use the original method's name.

**Metrics:** Recall@1/3/5, MRR@10, nDCG@5 (only with graded gold relevance), cross-script recall gap, retrieval latency (p50/p95), model footprint, query embedding time.

**Checklist:**
- [ ] Pin environment and model commit/revision, tokenizer, normalization, and random seed.
- [ ] Build a correct, deterministic BM25 pipeline (including Unicode handling).
- [ ] Freeze corpus chunk IDs + exact text hashes + metadata.
- [ ] Compute/cache document embeddings ONLY for approved sources.
- [ ] Run dense retrieval with identical top-K and corpus; check dimensionality and similarity normalization.
- [ ] Run hybrid retrieval; calibrate dense/sparse score scales or fuse ranks with validation only.
- [ ] Add verified transliteration/translation and fixed multi-view baselines if feasible.
- [ ] Output individual query failures, not only a mean score.
- [ ] Save per-language and per-family results and raw timing logs.
- [ ] **Gate B0:** At least three valid comparable retrieval systems (B1/B2/B3) have reproducible outputs on frozen pilot data.

**Proposed artifacts:** research/voicedoc/src/retrieval/{bm25,dense,hybrid}.py, experiments/retrieval_config.yaml, results/baselines/*.csv, environment.lock, run_manifest.json.

---

## 6. Phase M0 — Minimal constraint model, before complex algorithms

**Priority:** Demonstrate that topic matching alone can miss a relevant clinical relation. Start with **explicit, inspectable rule/slot extraction**. Do not begin with a custom Akshara articulatory transducer, an enormous SNOMED graph, or a fine-tuned NLI model.

**Proposed representation per query:** recognized concept(s), requested relation, negation and its scope, population qualifiers, quantity/units, duration/timeframe, evidence requirement(s), extraction uncertainty. Require relation-aware matching; a passage mentioning both "drug" and "pregnancy" is not sufficient unless it supports the relevant relation.

**First method:** bilingual curated term normalization + protected numeric/negation patterns + concept/relation matching; examine failures and only then consider phonetic-Akshara matching or a lightweight verified verifier.

**Critical corrections to the Deep Research report:**
- **Do not** treat sigmoid(raw cosine) as a calibrated probability or apply noisy-OR across weak matches; unrelated passages could become spuriously sufficient.
- **Do not** average mandatory constraints if any missing one must veto the answer; define mandatory vs optional fields explicitly.
- **Do not** blend incomparable dense, sparse, and entailment scores without calibration.
- A question is not an NLI proposition; convert to explicit testable hypothesis/required relation before verification.
- Opposite polarities in **different patient contexts or time scopes** do not necessarily contradict.
- Keep **urgency escalation** independent of sufficiency and contradiction classes; medical safeguards need separate review.
- An evidence pass does **not guarantee** that generated text is faithful: separately measure response quality and downstream checks.

**Checklist:**
- [ ] Build 30–50 pilot term/constraint mappings from verified resources, not fabricated terminology IDs.
- [ ] Implement Unicode preserving Telugu script; do not discard numbers/units or negation.
- [ ] Implement a deterministic extractor with traceable spans and confidence/error categories.
- [ ] Write unit tests for EN, TE, ROM, CM plus negation, quantities, and scope.
- [ ] Evaluate extraction precision, recall, and error categories on separately reviewed annotations.
- [ ] Build set-level support checks with explicit *requirement-to-passage evidence links*.
- [ ] Label unknown/unmatched concepts as **UNKNOWN**, not automatic proof of safety/sufficiency.
- [ ] Compare simple lexical/dense evidence thresholds with relation-aware sufficiency decisions.
- [ ] Run blind/independent adjudication where possible.
- [ ] **Gate M0:** Method must pass negative controls and show useful held-out discrimination beyond simple baselines. Otherwise revise or stop novelty claims.

**Proposed artifacts:** research/voicedoc/src/constraints/{lexicon,extract,relations}.py, src/sufficiency/gate.py, tests/test_constraints.py, tests/test_sufficiency.py, results/extraction_errors.csv.

---

## 7. Phase E0 — Experimental evaluation and ablations

### Controlled sufficiency experiments

Evaluate the **same family** across evidence states C0, C1, C2, C3a, C3b and across all four linguistic variants. Distinguish **retrieval evaluation** (choose passages from a corpus) from **controlled-context evaluation** (pass gold/perturbed evidence directly to verifier). Do not conflate them.

**Mandatory reference methods to implement honestly where feasible:**
- Best retrieval-score threshold.
- Lexical evidence coverage threshold.
- Entity-only coverage without relations.
- Proposed constraint-and-relation sufficiency.
- Prompt-only abstention on the same context if a suitable SLM is available.
- Established requirement-based sufficiency method if accessible; otherwise clearly document a reduced reimplementation and avoid head-to-head claims.

**Core metrics:** Sufficiency macro-F1 and AUROC (for binary supported vs not-supported scores), supported-answer recall, false abstention rate, correct abstention rate, contradictory-evidence recall, boundary-flip accuracy, selective-risk versus answer-coverage, confidence intervals clustered by family. Never call *abstention on insufficient context* a validated clinical safety rate without expert review.

### Ablations and controls

- [ ] **A1:** Exact text matching vs simple transliteration/normalization vs proposed constraint extraction.
- [ ] **A2:** Entity-only vs entity + relation vs relation + population/quantity/time/negation scope.
- [ ] **A3:** Single view vs always dual view vs conditional expansion (if routing implemented).
- [ ] **A4:** No sufficiency gate vs lexical threshold vs proposed verifier.
- [ ] **A5:** Noisy/distractor evidence and same-entity contradictory statements.
- [ ] **A6:** Unseen medical concepts and colloquial Romanized variants.
- [ ] **A7:** Post-generation check vs pre-generation gate (ONLY when generator and evaluator are held constant).
- [ ] **N1:** Randomly mismatched evidence must mostly be rejected; log remaining false support.
- [ ] **N2:** Negation, age, duration, units and dosage boundary swaps must be distinguished when genuinely meaningful.
- [ ] **N3:** Same keyword with different clinical relationship must not pass by entity overlap alone.
- [ ] **N4:** A missing essential modifier must not be accepted because a paragraph contains several topical words.

**Experimental integrity**
- [ ] Tune thresholds only on validation; freeze before evaluating test.
- [ ] Never optimize based on held-out test errors; create a new evaluation set for subsequent revisions.
- [ ] Use bootstrap confidence intervals resampled at **question-family level**, not 480 independent translations of 24 families.
- [ ] Report paired comparisons and the denominators behind every metric.
- [ ] Make statistical claims only with appropriate sample size and uncertainty.
- [ ] Record calibration curves or abstention tradeoffs and examples of both false support and false abstention.
- [ ] **Gate E0:** No research success claim without actual evaluation logs, configs, and independently checked gold labels.

---

## 8. Phase G0 — Generation, early exit, and mobile verification (after E0)

Once evidence assessment is validated:
1. Pass supported queries and cited passages to a clearly identified small model.
2. For unsupported or contradictory evidence, produce a cautious fixed informational fallback; never fabricate cited evidence.
3. Evaluate both **claims supported by passages** and the quality/appropriateness of abstentions.
4. Run generation and abstention for the *same* cases to measure actual compute savings, not just inferred savings.

**Guardrails:** Urgent-symptom handling is a separate conservative, reviewable policy; do not conflate it with contradictory evidence or declare clinical-grade triage. Only permitted, licensed medical information is bundled locally. User history is never used as a trusted knowledge document.

**Checkpoints:**
- [ ] Verify exact SLM, quantization artifact, tokenizer, runtime, license, offline capability.
- [ ] Benchmark Python/Kaggle separately from a real physical device.
- [ ] Implement source IDs and citation-to-passage validation in generation.
- [ ] Test unanswerable cases, irrelevant evidence, conflicting passages, and an empty local index.
- [ ] Verify the text pipeline in airplane mode after assets are installed.
- [ ] Measure model-load latency, retrieval latency, verifier latency, TTFT, decode tokens/second, total latency, peak RSS, thermal/energy if instrumentation permits.
- [ ] Run repeated device trials and warm/cold starts; record device model, RAM, OS, runtime, build/commit, model hashes.
- [ ] **Gate G0:** No numerical mobile claims without real repeated physical-device logs. No clinical-safety claim without qualified evaluation.

**Current target device:** OnePlus 11R 5G (confirm RAM variant and backend support; do not invent Android QNN, NPU, or LiteRT conversion success). The existing product plan remains the authority for Android component-by-component development.

---

## 9. Execution order: first pilot, then stronger study

| Window (target, not a guarantee) | Primary research work | Objective completion proof |
|---|---|---|
| **Oct 31, 2026** | D0 licensing/provenance; D1 pilot schema and 20–30 family plan; B0 code scaffolding | Source manifest, schema, sampled review set, first BM25 smoke test |
| **Nov 1, 2026** | B0 retrieval baselines; M0 minimal extractor and gate; negative tests | Baseline CSVs, unit tests, independently reviewed sample labels, error log |
| **Nov 2–3, 2026** | Controlled sufficiency tests, key ablations, provisional figures and manuscript outline | Explicit pilot results and limitations; draft is *not* considered validation |
| **Nov 4 onward** | More independently reviewed cases; stronger baselines; robustness, generation, hardware | Confirmed results / revised method; no pressured publication claims |

**Fallback logic:** If source permissions or labels are blocked, do not force a medical answer benchmark; prioritize an appropriately licensed terminology-level retrieval study with a narrower and honestly stated claim. If the constraint method fails strong baselines, retain the failure analysis, revise the hypothesis, or pivot to the benchmark/efficiency question instead of inventing improvements.

### Detailed immediate tasks — starting from zero

- [ ] **T01** Create a dedicated research directory after verifying repo state; don't delete app code.
- [ ] **T02** Make source/license evidence manifest; first commit should not include restricted content.
- [ ] **T03** Design JSON schema and annotation guide, including exact sufficiency label definitions.
- [ ] **T04** Create 5 hand-reviewed pilot families and write a validator that checks all variants and contexts.
- [ ] **T05** Expand to 20–30 families, keeping gold labels separately reviewed.
- [ ] **T06** Run BM25 on frozen approved passages and save per-query ranking.
- [ ] **T07** Run one multilingual dense baseline; validate embeddings and record checkpoint revision.
- [ ] **T08** Run a calibrated hybrid baseline with same documents and top-K.
- [ ] **T09** Audit where clinical modifiers, negation or relation context were lost.
- [ ] **T10** Implement the simplest constraint extractor and tests; avoid premature neural fine-tuning.
- [ ] **T11** Implement explicit requirement-to-evidence linking, tri-state scoring and unknown handling.
- [ ] **T12** Evaluate controlled-context sufficiency; compare lexical/score threshold.
- [ ] **T13** Add cross-script normalization/phonetic matching only if it fixes measured failures.
- [ ] **T14** Add conditional second-view retrieval only if it can be shown to save compute without a material retrieval loss.
- [ ] **T15** Run ablations, negative controls, error analysis and family-clustered uncertainty.
- [ ] **T16** Decide GO / REVISE / NO-GO based on results, not novelty rhetoric.
- [ ] **T17** Integrate the validated core behind existing VoiceDoc interfaces; test offline on a physical phone.
- [ ] **T18** Write paper sections ONLY using recorded results, proper citations and documented limitations.

---

## 10. Proposed research tree (DO NOT mistake this for existing files)

~~~text
research/
  voicedoc/
    README.md
    docs/
      DATA_GOVERNANCE.md
      LITERATURE_AND_NOVELTY.md
      EXPERIMENT_PROTOCOL.md
    data/
      sources_manifest.csv
      benchmark/
        schema.json
        annotation_guide.md
        families.jsonl
        evidence_contexts.jsonl
        splits.json
        dataset_card.md
    src/
      ingestion/
      retrieval/
        bm25.py
        dense.py
        hybrid.py
      constraints/
        lexicon.py
        extract.py
        relations.py
      sufficiency/
        gate.py
      evaluation/
        retrieval_metrics.py
        sufficiency_metrics.py
        bootstrap.py
    experiments/
      retrieval_config.yaml
      sufficiency_config.yaml
    tests/
      test_data_contract.py
      test_constraints.py
      test_sufficiency.py
      test_metrics.py
    results/
      baselines/
      ablations/
      timings/
      figures/
      run_manifest.json
~~~

Structure can be adjusted to the actual repository and tooling. **Nothing here is claimed to exist yet.** Do not commit licensed-restricted medical texts, full model weights, patient recordings, secrets, or large caches. Commit schemas, manifests, approved metadata, code, small allowed examples, results and reproducibility instructions.

---

## 11. Experiment reproducibility contract

Every result must include:
- Git commit SHA, Python environment/dependency lock, hardware/OS, exact model ID/revision/license and quantization format.
- Dataset/source version, split hashes, sample counts, exclusions, human-review status.
- Full command/config, random seeds, thresholds and whether chosen on validation.
- Per-family and per-script scores; not only aggregate mean.
- Raw predictions and timings with no private health content.
- Error categories, negative controls, confidence intervals where justified.
- Clearly labelled **measured**, **derived**, **simulated**, **literature-reported**, or **proposed** values.

**Never copy a previous study's reported score into our own Results table.** If results are poor, report them. A correct negative finding can still guide a useful product.

---

## 12. Definitions of done and review gates

| Gate | PASS requires | Status |
|---|---|---|
| **D0: Sources** | Reliable provenance + verified permission for intended use | ⬜ |
| **D1: Benchmark** | Versioned pilot, labels + review status + leakage checks | ⬜ |
| **B0: Retrieval** | BM25, dense, hybrid all reproducible on same test set | ⬜ |
| **M0: Constraint method** | Tested extraction, explicit relations, coherent unknown handling | ⬜ |
| **E0: Evidence** | Held-out comparison, negatives, ablations, uncertainty reporting | ⬜ |
| **G0: Generation/device** | Grounding checks + genuine offline physical-device benchmarks | ⬜ |
| **P0: Paper** | Claims map to real experiments and primary cited literature | ⬜ |

### Final go/no-go questions

1. Did we measure a real failure of baselines on English–Telugu constraint-aware evidence selection?
2. Did the proposed method improve on the strongest practical baselines and the closest published competitors?
3. Does the improvement remain under unseen concepts, colloquial expressions, and realistic perturbations?
4. Is improvement due to the actual clinical relation/constraint component, shown by ablation?
5. Are false acceptance and false abstention rates acceptable **for a research prototype**?
6. Are sources licensed appropriately and annotations independently verified?
7. Are latency, memory, and battery numbers measured on the hardware on which we claim them?
8. Could a reviewer reproduce results from the shared code and allowed artifacts?

If any mandatory answer is **NO**, report the blocker instead of claiming the research or system is finished.

---

## 13. Handoff: exact next task for a coding assistant

**Task R-01 — Build source governance + benchmark schema only.**

1. Audit the HEAD of this repository; preserve the existing Android app and docs.
2. Create research/voicedoc/ skeleton only as needed, with README and validation tests.
3. Implement sources_manifest.csv schema and DATA_GOVERNANCE.md with clear licensing states.
4. Implement benchmark/schema.json + annotation_guide.md for family/grouped language variants and five evidence states.
5. Implement a validator/test enforcing required fields, valid labels, same-family split, unique IDs, source provenance, and segregation of synthetic perturbations.
6. Add **only** 2–5 small, permitted illustrative examples and mark them synthetic/unreviewed unless independently verified.
7. Run tests and produce exact commands, results, changed paths, and commit SHA.
8. Do **not** train a model, create fictitious validated data, edit original README.md, or alter IMPLEMENTATION_PLAN.md in Task R-01.

**Task R-01 acceptance:** source manifest format + benchmark schema + tests compile/run, unknown-license data cannot be exported as redistributable, and no unverifiable medical claims enter the repo.

---

## 14. Notes on the 67-page research blueprint

The report supplied in this conversation is a **source of research hypotheses**, not implementation evidence. Its main proposal CS-CLINSUFF, TeMed-Suff benchmark, two-gate architecture and alternatives are useful to test. Important corrections before relying on it:

- Its statement that VoiceDoc is mainly a Python prototype was not supported by an actual README/code audit.
- Its characterization of SETU-RAG's routing, broad novelty labels, numerical predictions and confidence claims require correction against primary literature.
- Some proposed sufficiency scoring equations have failure modes and must not be copied uncritically.
- Source URLs and licenses (especially Vikaspedia, SNOMED CT, and downloadable benchmark artifacts) must be audited individually.
- Synthetic clinical examples are **not** clinician-verified medical ground truth.
- Physical-device speed and memory must be measured, not inferred from model file sizes or a Kaggle GPU run.

**Research motto:** Fix the question; verify the corpus; establish honest baselines; build the smallest method that can falsify the hypothesis; improve only where observed failures justify it.

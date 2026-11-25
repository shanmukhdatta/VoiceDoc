# Phase 06 — Independent bilingual safety / intent engine
**STATUS: RULE AND SCHEMA FOUNDATION, NOT MEDICALLY VALIDATED.**

- Kotlin-only safety intent (potential urgency, general information, uncertain input) runs without model.
- Bilingual static urgency and uncertainty notices for explicit examples; no promise to detect every emergency.
- Mechanical generated-output gate rejects empty/excessive output, sample high-risk certainty phrases, missing/unmatched citation IDs and unsigned citation requests.
- JVM tests: urgency examples in en/te, uncertain input, invented citations, overconfidence and acceptable evidence IDs.
- **Important:** an accepted gate result is NOT proof of medical correctness, semantic support, clinician approval or emergency exclusion. Unsafe language not in lists can still pass. Never rely on this alone for health advice.
- **Next:** qualified clinicians design/annotate high-risk case sets, calibrated sensitivity, code-switch/negation robustness, robust evidence entailment and appropriate risk escalation, retained held-out tests. Also integrate with eventual real text/voice pipeline.

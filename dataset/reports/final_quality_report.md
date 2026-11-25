# Candidate quality report

The audited release has 60 SFT candidates and 30 RAG chunks. The requested totals of 10,000 and 3,000 remain open. [Statistics](dataset_statistics.json) contains actual counts by domain, language, capability and split.

| Check | Result |
|---|---|
| Source scope | 15 registered MedlinePlus summaries. All content rows resolve to their documented Summary scopes. |
| Data structure | 0 errors from the committed validator. It checks required fields, metadata, language/script indicators, size bounds, structured turns, hashes, injected evidence and emergency action placement. |
| Duplicate scan | 0 exact normalized groups and 0 near pairs at character-trigram Jaccard ≥ 0.78 across prompts, responses and RAG text. The scan does not measure semantic equivalence or cross-language paraphrases. |
| Split integrity | All 60 SFT rows assigned once. No shared topic or scenario IDs across the 48/8/4 split. The test has one topic, so it cannot measure full-domain performance. |
| Native gate compatibility | Fifteen existing preparation, training and RAG unit tests passed. Five dataset integration tests passed. All candidate SFT and source-level RAG rows were rejected by the expected native clinician-review gates. |
| Language checks | Unicode script patterns and Arabic-digit sequence agreement checked. Telugu wording, spelled-out quantities, register and negations need qualified linguistic review. |
| Medical assessment | Same-session source checks during authoring. Six emergency SFT responses passed immediate-action heuristics. Qualified clinical assessment is pending. The project owner reported a satisfactory [agent review](agent_review.md). |
| Qwen3 tokenization | Validator support is present. The exact tokenizer audit remains pending because pinned assets and Transformers were unavailable in the execution environment. |
| Rejections | The final structural pass recorded zero rejections. Draft edits made during authoring were not tracked as independent records. |
| Approval | All 90 candidates have `clinician_reviewed: false` and `reviewed_by: null`. There are no approved training records or approved RAG chunks. |
| Evaluation | Eight candidate smoke prompts with distinct wording. Shared topics preclude treating this diagnostic set as an independent held-out benchmark. |

Source: MedlinePlus, National Library of Medicine. Dataset rights findings are scoped to the identified health-topic summaries and original adaptations; the relevant publisher policy is linked in the source registry. Source review dates are null where the cited Summary date was unavailable.

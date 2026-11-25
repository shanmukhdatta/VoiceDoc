# Dataset specification

Files are UTF-8. JSONL contains one JSON object per line. IDs are readable stable keys based on the source slug, scenario slug and language style. Scenario families and related source-topic examples share a split group. Keep translations, paraphrases and conversational variants in that group.

## SFT

| Fields | Meaning |
|---|---|
| `id`, `scenario_id`, `split_group` | Candidate identity, underlying scenario and leakage group |
| `domain`, `capability`, `risk_level` | Taxonomy keys, one primary capability and low/moderate/high triage priority |
| `language`, `language_style` | `en` for English or `te` for native Telugu, Telugu transliteration and Telugu-English code-switching. Style codes are `english`, `telugu`, `translit` and `code`. |
| `prompt`, `response` | Repository-compatible string fields. Multi-turn prompts flatten the conversation through the final user turn. |
| `conversation` | Optional alternating role/content messages including the final answer |
| `evidence_chunk_ids` | Grounded-answer context references. Supporting text appears verbatim from the identified candidate chunk inside the prompt. |
| `source_id`, `source_ids`, `license` | Primary source ID, all source IDs and scoped rights label |
| `synthetic`, `rights_verified`, `clinician_reviewed`, `reviewed_by` | Separate provenance and approval fields |

Capability codes are `general`, `symptom`, `emergency`, `grounded`, `clarify` and `multi`. Emergency responses put the immediate action first. Risk labels prioritize review and are insufficient to determine real patient urgency.

## RAG

| Fields | Meaning |
|---|---|
| `chunk_id`, `source_id`, `publisher`, `title`, `domain`, `language` | Chunk identity, registry link, bibliographic fields, taxonomy and language |
| `text`, `content_sha256` | Original factual restatement or AI-assisted Telugu rendering and SHA-256 of the exact UTF-8 text |
| `source_url`, `section_locator`, `publication_date`, `license` | Traceability and scope. The date is null when a publication or revision date could not be established for the Summary. |
| `translation_of` | English chunk ID for Telugu renderings |
| `rights_verified`, `clinician_reviewed`, `reviewed_by` | Scoped rights finding and independent clinical approval |

Source-level JSON is an array. Each source-language entry has a unique suffixed ID, registry source ID, contributing chunk IDs and a SHA-256 of its concatenated text. The existing SQLite builder uses 800-character windows internally. The source IDs are preserved in the pack and the index window IDs may differ from dataset chunk IDs.

Source registry hashes refer to the retrieved source evidence spans. RAG hashes refer to the published candidate text. The source registry records which fields and conditions the rights verification covers.

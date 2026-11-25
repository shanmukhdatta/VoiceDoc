# Generation progress

| Stage | Verified result |
|---|---|
| Reference inspection | Checked the required SFT preparation, training data loader and RAG builder |
| Source and rights collection | 1,017 English summaries extracted from the November 25, 2026 MedlinePlus archive. Of these, 916 additional source topics supplied English passages. Registry total is 936 sources. |
| Authored records | 80 SFT records and 20 English–Telugu passage pairs |
| Archive expansion | 1,480 additional English chunks, 10 character-trigram near matches excluded |
| Materialization | 80 SFT candidates, 1,520 RAG chunks, 956 candidate source-language entries, 8 smoke evaluation prompts |
| Validation | 0 structural errors, 0 exact duplicate groups, 0 final trigram near-duplicate pairs |
| Split | 64 train, 8 validation, 8 test, grouped by source topic |
| Review status | 1,600 records queued, 0 clinician approved |
| Remaining generation | 9,920 SFT candidates and 1,480 Telugu RAG chunks |

The English RAG target was reached using the publisher's archive. The archive manifest records the exact XML snapshot and extraction method. Passages retain the publisher's English wording and may contain U.S.-specific references. Telugu batch generation and expanded SFT require a suitable teacher runtime. The local Ollama server could not bind, and the only installed model found was the intended Qwen3 0.6B student. No training or inference benchmarks were run.

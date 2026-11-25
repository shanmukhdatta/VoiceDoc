# Generation progress

| Stage | Observed result |
|---|---|
| Reference inspection | Inspected SFT preparation, QLoRA training data loader and RAG builder at the specified reference revision |
| Source and rights collection | 15 MedlinePlus health-topic Summary scopes registered with publisher policy, section locators and retrieved span hashes |
| Authored batch | 60 distinct SFT scenarios and 15 factual passage pairs maintained as reproducible TSV inputs |
| Materialization | 60 SFT candidates, 30 RAG chunks, 30 candidate source-language entries, 8 smoke evaluation prompts |
| Validation | 0 final structural validation errors, 0 exact duplicate groups and 0 trigrams-based near-duplicate pairs |
| Split | 48 train, 8 validation, 4 test, grouped by topic with seed 42 |
| Review status | 90 records queued, 0 clinician approved |
| Remaining generation | 9,940 SFT candidates and 2,970 RAG chunks |

The batch was authored directly in this session with no paid API use. The generation manifest contains input and materialized dataset hashes. Additional genuinely authored batches can build on these inputs. Time and generation capacity in this execution constrained how many source-backed records could be individually authored and checked. Expanding coverage requires additional compliant reference sections, scenario writing and quantitative/qualitative checks.

# Generation progress

| Stage | Observed result |
|---|---|
| Reference inspection | Inspected SFT preparation, QLoRA training data loader and RAG builder at the specified reference revision |
| Source and rights collection | 20 MedlinePlus health-topic Summary scopes registered with publisher policy, section locators and retrieved span hashes |
| Authored batch | 80 distinct SFT scenarios and 20 factual passage pairs maintained as reproducible TSV inputs |
| Materialization | 80 SFT candidates, 40 RAG chunks, 40 candidate source-language entries, 8 smoke evaluation prompts |
| Validation | 0 final structural validation errors, 0 exact duplicate groups and 0 trigrams-based near-duplicate pairs |
| Split | 64 train, 8 validation, 8 test, grouped by topic with seed 42 |
| Review status | 120 records queued, 0 clinician approved |
| Remaining generation | 9,920 SFT candidates and 2,960 RAG chunks |

The first batch and expansion were authored directly with no paid API use. The expansion added five sources, 20 SFT candidates and 10 bilingual RAG chunks. The generation manifest contains input and materialized dataset hashes. Additional genuinely authored batches can build on these inputs. The local model server could not bind to localhost in the execution environment. The only installed Ollama model was the Qwen3 0.6B student model. Shell DNS resolution also blocked the authoring process from downloading the full publisher archive. The XML extractor was exercised on controlled inputs. Expanding coverage requires additional compliant reference sections, scenario writing and quantitative/qualitative checks.

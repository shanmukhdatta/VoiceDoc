# VoiceDoc candidate dataset card

The SFT candidates teach concise health-information conversation, clarification, evidence use and urgent escalation. The RAG passages provide traceable background information. Use these files for engineering and review pending explicit approval for downstream use.

| Data | Actual | Requested | Approved |
|---|---:|---:|---:|
| SFT | 80 | 10,000 | 0 |
| RAG | 1,520 | 3,000 | 0 |

Across all 15 domains, the authored seed covers 20 source topics, with four SFT records and one English–Telugu RAG pair per topic. The archive expansion adds 1,480 English chunks across 916 additional source topics. The scenarios are fictional and use no identifiable patient information. Candidate English and Telugu prose was authored with ChatGPT in Codex Work Mode. The teacher manifest records the available GPT-6-family label and leaves its precise snapshot unknown.

The source registry covers 936 MedlinePlus health-topic summaries under the publisher's [content policy](https://medlineplus.gov/about/using/usingcontent/). Source: MedlinePlus, National Library of Medicine. Published passages include original restatements, Telugu adaptations and English extracts from the publisher archive. Authored emergency examples use local services. Archive extracts may contain U.S.-specific numbers or guidance and require localization review.

Qualified clinical and native Telugu review remain pending. Script checks and same-session source review catch specific issues but cannot establish clinical accuracy, detect every paraphrase, or validate translations. The source set lacks India-specific care pathways, pediatric depth, locality-specific immunization schedules and broad emergency coverage. This release adds suspected heart attack and severe allergic reaction scenarios to the earlier escalation examples.

"""Materialize independently authored candidate batches. No API calls."""
import argparse
import csv
from itertools import chain
from pathlib import Path
import re

from common import ROOT, SFT, RAG, STYLES, canonical, digest, has_rights, sources, write_json, write_rows

AUTHORING = ROOT / "sources/authoring"
ROLE_LABELS = {"en": ("Speaker", "Assistant", "Reference"), "te": ("ప్రశ్న", "సహాయకుడు", "ఆధారం")}


def cells(path):
    with path.open(encoding="utf-8", newline="") as file:
        yield from csv.reader(file, delimiter="|", quoting=csv.QUOTE_NONE)


def slug(value):
    if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", value):
        raise ValueError(f"Invalid stable identifier: {value}")
    return value


def topic_key(value):
    topic, separator, facet = value.partition("@")
    return slug(topic), slug(facet) if separator else ""


def chunk_key(topic, facet, language):
    stem = f"{topic}-{facet}" if facet else topic
    return f"rag-{stem}-{language}"


def materialize(authoring=AUTHORING):
    registry = sources()
    rag = {}
    archive = authoring / "rag_archive_en.tsv"
    authored = ((topic, (("en", en), ("te", te))) for topic, en, te in cells(authoring / "rag.tsv"))
    extracted = ((topic, (("en", en),)) for topic, en in cells(archive)) if archive.is_file() else ()
    for topic_value, passages in chain(authored, extracted):
        topic, facet = topic_key(topic_value)
        origin = registry["nlm-" + topic]
        for language, passage in passages:
            key = chunk_key(topic, facet, language)
            if (topic, facet, language) in rag:
                raise ValueError(f"Repeated chunk identity: {key}")
            rag[topic, facet, language] = {
                "chunk_id": key, "source_id": origin["source_id"], "publisher": origin["publisher"],
                "title": origin["title"], "domain": origin["domain"], "language": language,
                "text": passage, "source_url": origin["url"], "section_locator": origin["section_locator"],
                "publication_date": origin["publication_date"], "license": origin["license"],
                "content_sha256": digest(passage), "rights_verified": bool(has_rights(origin)),
                "clinician_reviewed": False, "reviewed_by": None, "translation_of":
                chunk_key(topic, facet, "en") if language == "te" else None,
            }

    sft = []
    identities = set()
    chunk_ids = [row["chunk_id"] for row in rag.values()]
    if len(chunk_ids) != len(set(chunk_ids)):
        raise ValueError("Topic and facet slugs collide in a RAG chunk ID")
    for topic_value, scenario, capability, style, risk, user, answer, *tail in cells(authoring / "sft.tsv"):
        if len(tail) != (1 if capability == "multi" else 0):
            raise ValueError(f"{topic_value}/{scenario}: unexpected conversation history")
        topic, facet = topic_key(topic_value)
        origin = registry["nlm-" + topic]
        scenario_id = f"{topic}-{slug(scenario)}"
        key = f"sft-{scenario_id}-{style}"
        if key in identities:
            raise ValueError(f"Repeated example identity: {key}")
        identities.add(key)
        lang = STYLES[style]
        user_label, assistant_label, context_label = ROLE_LABELS["en" if style == "translit" else lang]
        record = {
            "id": key, "scenario_id": scenario_id, "split_group": topic, "domain": origin["domain"],
            "capability": capability, "language": lang, "language_style": style, "risk_level": risk,
            "prompt": user, "response": answer, "source_id": origin["source_id"],
            "source_ids": [origin["source_id"]], "license": origin["license"], "synthetic": True,
            "rights_verified": bool(has_rights(origin)), "clinician_reviewed": False, "reviewed_by": None,
        }
        if capability == "grounded":
            evidence = rag[topic, facet, "en" if style == "translit" else lang]
            record["evidence_chunk_ids"] = [evidence["chunk_id"]]
            record["prompt"] = f"{context_label}: {evidence['text']}\n\n{user_label}: {user}"
        if capability == "multi":
            history = tail[0].split("~")
            if len(history) != 3 or not all(value.strip() for value in history):
                raise ValueError(f"{key}: expected three prior utterances")
            turns = [
                {"role": "user", "content": history[0]},
                {"role": "assistant", "content": history[1]},
                {"role": "user", "content": f"{history[2]} {user}"},
            ]
            record["conversation"] = turns + [{"role": "assistant", "content": answer}]
            labels = {"user": user_label, "assistant": assistant_label}
            record["prompt"] = "\n".join(f"{labels[turn['role']]}: {turn['content']}" for turn in turns)
        sft.append(record)
    return sorted(sft, key=lambda row: row["id"]), sorted(rag.values(), key=lambda row: row["chunk_id"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--authoring", type=Path, default=AUTHORING)
    args = parser.parse_args()
    sft, rag = materialize(args.authoring)
    for path, rows in ((SFT, sft), (RAG, rag)):
        write_rows(path, rows)
    taxonomy = {}
    for row in sft:
        topic = row["split_group"]
        scenario = row["scenario_id"][len(topic) + 1:]
        metadata = taxonomy.setdefault(topic, {}).setdefault(scenario, {
            "primary_capability": row["capability"], "risk_level": row["risk_level"],
            "language_styles": [],
        })
        if metadata["primary_capability"] != row["capability"] or metadata["risk_level"] != row["risk_level"]:
            raise ValueError(f"{row['scenario_id']}: inconsistent scenario metadata")
        metadata["language_styles"].append(row["language_style"])
    write_json(ROOT / "taxonomy/scenario_taxonomy.json", taxonomy)
    write_json(ROOT / "reports/generation_manifest.json", {
        "teacher": "ChatGPT in Codex Work Mode (GPT-6 family)",
        "teacher_snapshot": None,
        "method": "Individually authored SFT and bilingual seed passages, with extractive English archive passages when present",
        "batch_inputs": {
            name: digest((args.authoring / name).read_text(encoding="utf-8"))
            for name in ("sft.tsv", "rag.tsv", "rag_archive_en.tsv") if (args.authoring / name).is_file()
        },
        "registry_sha256": digest((ROOT / "sources/source_registry.json").read_text(encoding="utf-8")),
        "sft_records": len(sft), "rag_chunks": len(rag),
        "sft_rows_sha256": digest(canonical(sft)), "rag_rows_sha256": digest(canonical(rag)),
    })
    print(f"Materialized {len(sft)} SFT candidates and {len(rag)} RAG chunks.")


if __name__ == "__main__":
    main()

"""Extract designated English MedlinePlus Health Topic Summaries into a local evidence file."""
import argparse
import hashlib
from datetime import date
from pathlib import Path
import re
from urllib.parse import urlsplit
from xml.etree import ElementTree
from zipfile import ZipFile, is_zipfile

from common import digest, write_rows

MAX_XML_BYTES = 64 * 1024 * 1024
TOPIC_PATH = re.compile(r"/([a-z0-9]+(?:-[a-z0-9]+)*)\.html\Z")
POLICY = "https://medlineplus.gov/about/using/usingcontent/"


def xml_bytes(path):
    if is_zipfile(path):
        with ZipFile(path) as archive:
            items = [f for f in archive.infolist() if not f.is_dir() and f.filename.endswith(".xml")]
            if len(items) != 1 or not 0 < items[0].file_size <= MAX_XML_BYTES:
                raise ValueError("Expected one suitably sized Health Topic XML file")
            return archive.read(items[0])
    if not 0 < path.stat().st_size <= MAX_XML_BYTES:
        raise ValueError("Unexpected XML size")
    return path.read_bytes()


def extract(snapshot, accessed):
    if re.search(rb"<!\s*ENTITY\b", snapshot, re.I):
        raise ValueError("Entity declarations are outside the import format")
    snapshot_hash = hashlib.sha256(snapshot).hexdigest()
    root = ElementTree.fromstring(snapshot)
    if root.tag != "health-topics":
        raise ValueError("Expected a health-topics XML document")
    rows = {}
    for topic in root.findall("health-topic"):
        if topic.get("language") != "English":
            continue
        url = topic.get("url", "")
        parsed = urlsplit(url)
        match = TOPIC_PATH.fullmatch(parsed.path)
        if (parsed.scheme, parsed.netloc) != ("https", "medlineplus.gov") or parsed.query or parsed.fragment or not match:
            continue
        summary = topic.find("full-summary")
        title = topic.get("title", "").strip()
        if summary is None or not title:
            continue
        evidence = " ".join(" ".join(summary.itertext()).split())
        if not evidence:
            continue
        slug = match[1]
        if slug in rows:
            raise ValueError(f"Duplicate English topic slug: {slug}")
        rows[slug] = {
            "source_id": f"nlm-{slug}", "title": title, "url": url,
            "publisher": "U.S. National Library of Medicine (MedlinePlus)",
            "section_locator": "Summary, MedlinePlus Health Topic XML full-summary",
            "summary_text": evidence, "content_sha256": digest(evidence),
            "hash_scope": "Whitespace-normalized text from the full-summary element",
            "snapshot_sha256": snapshot_hash, "topic_created": topic.get("date-created"),
            "publication_date": None, "access_date": accessed,
            "source_groups": sorted({g.text.strip() for g in topic.findall("group") if g.text and g.text.strip()}),
            "license_evidence": POLICY,
        }
    if not rows:
        raise ValueError("No eligible English health-topic summaries")
    return [rows[key] for key in sorted(rows)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--access-date", type=date.fromisoformat, required=True)
    args = parser.parse_args()
    rows = extract(xml_bytes(args.input), args.access_date.isoformat())
    write_rows(args.output, rows)
    print(f"Extracted {len(rows)} source summaries for local evidence preparation.")


if __name__ == "__main__":
    main()

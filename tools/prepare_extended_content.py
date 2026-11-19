#!/usr/bin/env python3
"""Build deterministic, ASCII transport files from approved upstream resources."""

import argparse
import base64
import hashlib
import json
import re
import sqlite3
from pathlib import Path


def b64(value: str | None) -> str:
    return base64.b64encode((value or "").encode("utf-8")).decode("ascii")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def verse_keys(quran_text: Path) -> set[str]:
    keys: set[str] = set()
    with quran_text.open(encoding="utf-8") as source:
        for raw in source:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            surah, ayah, _ = line.split("|", 2)
            keys.add(f"{int(surah)}:{int(ayah)}")
    if len(keys) != 6236:
        raise ValueError(f"Expected 6236 Quran keys, received {len(keys)}")
    return keys


def write_translation(database: Path, destination: Path, keys: set[str]) -> None:
    connection = sqlite3.connect(f"file:{database.as_posix()}?mode=ro", uri=True)
    try:
        rows = connection.execute(
            "SELECT sura, aya, translation, footnotes FROM translations ORDER BY sura, aya"
        ).fetchall()
    finally:
        connection.close()
    found = {f"{int(row[0])}:{int(row[1])}" for row in rows}
    if len(rows) != 6236 or found != keys:
        raise ValueError("QuranEnc translation does not cover the exact 6236-ayah key set")
    with destination.open("w", encoding="ascii", newline="\n") as target:
        target.write("# QuranEnc french_rashid 1.0.3\n")
        target.write("# Source: https://quranenc.com/downloads/sqlite/french_rashid.sqlite\n")
        target.write(f"# Source-SHA256: {sha256(database)}\n")
        target.write("# Fields: verseKey|translationBase64Utf8|footnotesBase64Utf8\n")
        for surah, ayah, translation, footnotes in rows:
            if not translation:
                raise ValueError(f"Empty translation at {surah}:{ayah}")
            target.write(f"{int(surah)}:{int(ayah)}|{b64(translation)}|{b64(footnotes)}\n")


def write_audio_manifest(source_manifest: Path, destination: Path, keys: set[str]) -> None:
    data = json.loads(source_manifest.read_text(encoding="utf-8"))
    columns = {name: index for index, name in enumerate(data["columns"])}
    selected: dict[str, tuple[int, str, str]] = {}
    pattern = re.compile(r"^(\d{3})(\d{3})\.mp3$")
    for row in data["files"]:
        match = pattern.match(row[columns["name"]])
        if not match or match.group(2) == "000":
            continue
        key = f"{int(match.group(1))}:{int(match.group(2))}"
        if key not in keys:
            continue
        url = f"https://everyayah.com/data/Alafasy_128kbps/{match.group(0)}"
        selected[key] = (int(row[columns["bytes"]]), row[columns["sha256"]], url)
    if set(selected) != keys:
        missing = sorted(keys - set(selected))[:5]
        raise ValueError(f"Audio manifest does not cover the Quran key set; missing {missing}")
    with destination.open("w", encoding="ascii", newline="\n") as target:
        target.write("# Mishari Rashid Alafasy 128kbps file manifest\n")
        target.write("# Audio hosted by EveryAyah; SHA-256 inventory by Maqra\n")
        target.write(f"# Source-SHA256: {sha256(source_manifest)}\n")
        target.write("# Fields: verseKey|bytes|sha256|remoteUrl\n")
        for key in sorted(selected, key=lambda item: tuple(map(int, item.split(":")))):
            size, checksum, url = selected[key]
            target.write(f"{key}|{size}|{checksum}|{url}\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--quran-text", type=Path, required=True)
    parser.add_argument("--quranenc-db", type=Path, required=True)
    parser.add_argument("--audio-manifest", type=Path, required=True)
    parser.add_argument("--translation-out", type=Path, required=True)
    parser.add_argument("--audio-out", type=Path, required=True)
    args = parser.parse_args()
    keys = verse_keys(args.quran_text)
    args.translation_out.parent.mkdir(parents=True, exist_ok=True)
    args.audio_out.parent.mkdir(parents=True, exist_ok=True)
    write_translation(args.quranenc_db, args.translation_out, keys)
    write_audio_manifest(args.audio_manifest, args.audio_out, keys)
    print(f"translation_sha256={sha256(args.translation_out)}")
    print(f"audio_manifest_sha256={sha256(args.audio_out)}")


if __name__ == "__main__":
    main()

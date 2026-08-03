#!/usr/bin/env python3
"""Append a tamper-evident, hash-chained entry to prompts/prompt-history.md.

Each entry records one turn (a user prompt and the action taken) and links to
the previous entry via a SHA-256 hash. Editing or reordering any earlier entry
breaks the chain, which the scorer detects.

Usage:
    python prompts/log.py "<verbatim prompt>" "<one-line action>"
    python prompts/log.py --verify        # check the whole chain

Stdlib only; runs the same on macOS, Linux, and Windows.
"""
from __future__ import annotations

import hashlib
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

HISTORY = Path(__file__).with_name("prompt-history.md")
GENESIS = "GENESIS"
ENTRY_RE = re.compile(
    r"^### (?P<ts>.+)\n"
    r"\*\*Prompt:\*\* (?P<prompt>.*)\n"
    r"\*\*Action:\*\* (?P<action>.*)\n"
    r"\*\*Prev:\*\* (?P<prev>.+)\n"
    r"\*\*Hash:\*\* (?P<hash>[0-9a-f]{64})$",
    re.MULTILINE,
)


def _digest(prev: str, ts: str, prompt: str, action: str) -> str:
    payload = "\n".join([prev, ts, prompt, action]).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _one_line(text: str) -> str:
    return " ".join(text.split())


def parse_entries(text: str) -> list[dict]:
    return [m.groupdict() for m in ENTRY_RE.finditer(text)]


def last_hash(text: str) -> str:
    entries = parse_entries(text)
    return entries[-1]["hash"] if entries else GENESIS


def append(prompt: str, action: str) -> None:
    text = HISTORY.read_text(encoding="utf-8") if HISTORY.exists() else ""
    prev = last_hash(text)
    ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    prompt, action = _one_line(prompt), _one_line(action)
    digest = _digest(prev, ts, prompt, action)
    block = (
        f"### {ts}\n"
        f"**Prompt:** {prompt}\n"
        f"**Action:** {action}\n"
        f"**Prev:** {prev}\n"
        f"**Hash:** {digest}\n"
    )
    sep = "" if not text or text.endswith("\n\n") else ("\n" if text.endswith("\n") else "\n\n")
    # newline="" keeps LF on every OS (no Windows CRLF translation), so the file
    # stays byte-identical to what log.ps1 / log.sh write.
    with open(HISTORY, "w", encoding="utf-8", newline="") as f:
        f.write(text + sep + block + "\n")
    print(f"logged {ts} ({digest[:12]}...)")


def verify() -> int:
    if not HISTORY.exists():
        print("no prompt-history.md found")
        return 1
    entries = parse_entries(HISTORY.read_text(encoding="utf-8"))
    if not entries:
        print("no hash-chained entries found")
        return 1
    prev = GENESIS
    last_ts = ""
    for i, e in enumerate(entries):
        if e["prev"] != prev:
            print(f"BROKEN CHAIN at entry {i + 1}: prev mismatch")
            return 1
        want = _digest(e["prev"], e["ts"], _one_line(e["prompt"]), _one_line(e["action"]))
        if want != e["hash"]:
            print(f"TAMPERED at entry {i + 1}: hash mismatch")
            return 1
        if e["ts"] < last_ts:
            print(f"NON-MONOTONIC timestamp at entry {i + 1}: {e['ts']} < {last_ts}")
            return 1
        last_ts = e["ts"]
        prev = e["hash"]
    print(f"OK: {len(entries)} entries form a valid chain")
    return 0


def main(argv: list[str]) -> int:
    if len(argv) == 2 and argv[1] == "--verify":
        return verify()
    if len(argv) != 3:
        print(__doc__)
        return 2
    append(argv[1], argv[2])
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))

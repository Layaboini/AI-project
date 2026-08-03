# Agent instructions

These instructions apply to any AI coding assistant used in this repository
(GitHub Copilot, CLI agents, etc.).

## Prompt logging (mandatory, do this first)

Before acting on a request, append a hash-chained entry to
`prompts/prompt-history.md` using the logger for your platform (no Python
required — pick the one that matches your shell):

```
# macOS / Linux (and Git Bash / WSL on Windows)
sh prompts/log.sh "<the user's request, verbatim>" "<one-line action>"

# Windows PowerShell (or PowerShell 7 on any OS)
pwsh -NoProfile -File prompts/log.ps1 "<the user's request, verbatim>" "<one-line action>"
```

All loggers write the same format, so the choice is purely about which shell you
have. (`python prompts/log.py "<prompt>" "<action>"` remains available as an
optional fallback if you prefer Python.)

Each helper stamps a UTC ISO-8601 timestamp and links each entry to the previous
one with a SHA-256 hash (fields: `Prompt`, `Action`, `Prev`, `Hash`), producing a
tamper-evident record.

Append only, via the helper — never edit, reorder, or delete earlier entries;
that breaks the chain, which the scorer flags. Log **every turn**, including
follow-ups and any clarifying question you ask plus the candidate's answer
(record both as their own entries) so the reasoning is preserved, not just the
opening prompt. Verify anytime with `sh prompts/log.sh --verify` (or
`pwsh -NoProfile -File prompts/log.ps1 -Verify`).

## Chat session export (mandatory, do this at the end of every session)

The prompt log and your exported chat session are **dual records** — the scorer
cross-checks them for integrity and cross-candidate plagiarism. You must export
the chat session and include it in the submission:

- **`Ctrl`+`Shift`+`P`** → **`Chat: Export Chat`** → save into the **`out/`** folder
  (internal: the workspace transcript folder also works; BYOAI: any exported chat
  transcript).

A missing/empty transcript makes the submission incomplete; a transcript that
disagrees with the prompt log is an integrity flag.

## Working style

- Use the datasets in `data/` and the task specs in `problems/`.
- Keep all work inside this repository.

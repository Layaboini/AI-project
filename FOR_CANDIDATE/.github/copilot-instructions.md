# Copilot instructions for this repository

These instructions apply to every request in this repository.

## Prompt logging (mandatory, do this first)

Before doing anything else for a request, append a hash-chained entry to
`prompts/prompt-history.md` using the logger for your platform (no Python
required — pick the one that matches your shell):

```
# macOS / Linux (and Git Bash / WSL on Windows)
sh prompts/log.sh "<the user's request, verbatim>" "<one short line: what you did>"

# Windows PowerShell (or PowerShell 7 on any OS)
pwsh -NoProfile -File prompts/log.ps1 "<the user's request, verbatim>" "<one short line: what you did>"
```

All loggers write the same format, so the choice is purely about which shell you
have. (`python prompts/log.py "<prompt>" "<action>"` remains available as an
optional fallback if you prefer Python.)

Each helper stamps a UTC ISO-8601 timestamp and links each entry to the previous
one with a SHA-256 hash, producing a tamper-evident record:

```
### <UTC timestamp, ISO 8601>
**Prompt:** <the user's request, verbatim>
**Action:** <one short line describing what you did>
**Prev:** <hash of the previous entry, or GENESIS>
**Hash:** <sha256 of this entry>
```

Rules:
- Append only, via the helper. Never edit, reorder, or delete earlier entries —
  doing so breaks the chain, which the scorer flags.
- Log **every turn**: follow-ups, small requests, and any clarifying question you
  ask plus the candidate's answer. Record the question and the reply as their own
  entries so the reasoning is preserved, not just the opening prompt.
- Timestamps must be non-decreasing. Verify anytime with
  `sh prompts/log.sh --verify` (or `pwsh -NoProfile -File prompts/log.ps1 -Verify`).
- Keep your AI session log (internal: the workspace session/transcript folder;
  BYOAI: an exported chat transcript) and include it in your submission.

## Working style

- Use the datasets in `data/` and the task specs in `problems/`.
- Keep solutions self-contained in this repository.

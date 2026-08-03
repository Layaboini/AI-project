# Trade Proceeds — AI-Assisted Coding Eval

This exercise evaluates how effectively you apply AI to realistic engineering
work, rather than testing recall or typing speed. You are expected to use an AI
coding assistant throughout. The work is organised into two tracks:

- **Track A — Phases 1–3:** build a small trade-proceeds tool, one phase at a
  time. Complete the phases in order, and attempt as many as are appropriate for
  the role you are interviewing for. Phases 2 and 3 build on Phase 1 and are
  progressively more advanced; they are not all mandatory.
- **Track B — Phase 4:** Drive the work with
  **AI skills and agents that you author**. This track assesses applied AI and is
  scored independently of Track A.

## Time box

The exercise is **self-timed** and offline. Plan for **about 3–4 hours total**;
you are **not** expected to finish everything. A smaller amount of well-reasoned,
well-verified work beats a rushed full run — reaching Phase 3 cleanly is stronger
than a broken attempt at all four. Rough guidance: Phase 1 ~30 min, Phase 2
~45 min, Phase 3 ~90 min, Phase 4 ~60–75 min. **Note where you stopped** in your
submission.

> ⚠️ REQUIRED: EXPORT YOUR CHAT SESSION INTO THE OUTPUT FOLDER ⚠️
>
> Your submission **must** include your exported chat session history. The fastest
> way: press **`Ctrl`+`Shift`+`P`** → type **`Chat: Export Chat`** → save the file
> into your submission's **`out/`** folder (alongside `prompts/prompt-history.md`).
>
> **A submission without an exported chat session is incomplete and cannot be
> scored.** .
>
> (Docs: [Save and export chat sessions](https://code.visualstudio.com/docs/chat/chat-sessions#_save-and-export-chat-sessions).)

## Setting up your AI assistant

**Internal (Microsoft):** VS Code with GitHub Copilot. No setup is required beyond
the Copilot extension; your prompts and session are captured automatically.

**External (bring your own AI):** any AI coding assistant in VS Code is acceptable.
A free or trial tier is sufficient for this exercise:
- GitHub Copilot Free — https://github.com/features/copilot
- GitHub Copilot Pro 30-day trial — https://github.com/features/copilot/plans
- Cursor free/trial — https://www.cursor.com

> **Quick tip:** attach the specific files or functions you are working on rather
> than pasting whole files — it is more economical with tokens and is better
> practice. You must supply your own prompt log and a chat transcript export (see
> below).

## Intentionally open requirements

A number of requirements are deliberately under-specified; there is no single
hidden "correct" answer. We are interested in whether you **notice** the open
question, **decide** sensibly, and **record the assumption** in a short
`DECISIONS.md`. A clear, stated assumption is exactly what we are looking for;
silently guessing is not.

## Recording your prompts

Your assistant logs each turn to `prompts/prompt-history.md` (see
`.github/copilot-instructions.md` and `AGENTS.md`). Retain this file, as it forms
part of your submission.

**You must also export your full chat transcript** — the log captures your prompts,
whereas the transcript captures the assistant's clarifying questions and your
answers. Two ways to export, easiest first:

1. **Shortcut (recommended):** **`Ctrl`+`Shift`+`P`** → **`Chat: Export Chat`** →
   save into your **`out/`** folder.
2. Follow
   [Save and export chat sessions](https://code.visualstudio.com/docs/chat/chat-sessions#_save-and-export-chat-sessions).

> **Do this at the end of every session.** A missing or empty transcript makes the
> submission incomplete, and a transcript that disagrees with the prompt log is an
> integrity flag.

## Track A — the phases (complete in order)

Each phase extends the code from the previous one.

1. [problems/phase-1.md](problems/phase-1.md) — proceeds per account.
2. [problems/phase-2.md](problems/phase-2.md) — monthly traded value by region.
3. [problems/phase-3.md](problems/phase-3.md) — event-driven trade service.

Use a single language for Track A: **Java** or **.NET (C#)**. Write outputs into an
`out/` folder as described in each phase.

Output format: JSON emits monetary values as a **JSON number** (`60` and `60.0`
are equivalent); CSV monetary fields use **exactly two decimal places**.

### Data
- `data/trades.csv` — trades (all phases; contains some malformed rows).
- `data/accounts.csv` — account → region (Phase 2).
- `data/events/*.json` — trade lifecycle events (Phase 3).

### Worked examples — use these to check your work

Each line below gives sample input file(s) on the left and the exact output your
program should produce on the right (`→` means "produces"). Run your solution on
the listed input and confirm the result matches the given output file exactly —
if it does, your logic for that phase is correct.

- `examples/phase-1-input.csv` → `examples/phase-1-output.json`
- `examples/phase-2-input.csv` + `examples/phase-2-accounts.csv` → `examples/phase-2-output.csv`
- `examples/phase-3-base-trades.csv` + `examples/phase-3-events.json` → `examples/phase-3-query-output.json`

## Track B — Phase 4

See [problems/phase-4.md](problems/phase-4.md). The C# engine is located in
`phase4/` and currently builds and passes its tests. Read it before you begin
prompting. You must author and submit at least one skill and one agent (example
stubs are provided, marked "EXAMPLE ONLY — replace with your own"), together with
`WORKFLOW.md` and `SCALE.md`.

## Submission checklist

Submit a **Git bundle** (.bundle) containing the complete repository history and all required deliverables. Exclude build artifacts and secrets before creating the bundle.

- [ ] Track A source code and `out/` outputs
- [ ] `DECISIONS.md` (open questions and your assumptions); Phase 3 `DESIGN.md`
- [ ] Track B: bug fix, feature, and tests; your authored `phase4/.github/skills`
      and `phase4/.github/agents`; `phase4/WORKFLOW.md`; `phase4/SCALE.md`
- [ ] `prompts/prompt-history.md` (do not edit earlier entries)
- [ ] **Exported chat session/transcript saved into `out/`** (`Ctrl`+`Shift`+`P`
      → `Chat: Export Chat`) — **required; submission is incomplete without it**
- [ ] A brief note indicating where you stopped

## Git Bundle Requirements
Create a Git bundle with your name that contains all commits required to review your submission. Example `jane_doe.bundle`
```
git bundle create <candidate>.bundle --all
```
Before submitting, verify the bundle can be cloned:
```
git clone <candidate>.bundle submission-review
```
Submit the resulting .bundle file as your final deliverable

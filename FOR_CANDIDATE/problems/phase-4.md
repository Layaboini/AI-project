# Phase 4 — The AI-Enabled Session (Applied AI)

This phase is **different**. Phases 1–3 level you; Phase 4 measures how you
**apply AI** on an unfamiliar codebase — the way a real AI-assisted engineering
session works. It does not depend on your Phase 1–3 code.

You are dropped into a small but multi-file **C# trade-processing engine** in
`phase4/`. It builds, runs, and has passing tests today. In one session you will
**fix a bug, add a feature, and make it scale** — driving the work with AI you
direct, not AI that drives you.

The engine builds current trade state from a base blotter plus a stream of
lifecycle **events** (`new`, `fill`, `cancel`, `bust`), which it replays in
timestamp order. `phase4/README.md` documents this data model — read it before
you start; you do **not** need anything from Phases 1–3 to work here.

Time box: **~60–75 minutes.** Partial credit is real — a clean 4a beats a broken
4c. Do the checkpoints in order.

## Setup
```
cd phase4
dotnet build
dotnet test
dotnet run --project src -- top-accounts --start 2024-05-01 --end 2024-05-31 --limit 5 --offset 0
```
Read `phase4/README.md` for the layout. Read the code before you prompt.

## The three tasks

### 4a — Fix a bug
Applying the events double-counts one trade. Find it (it spans more than one
file), fix it, and add a **test that fails before your fix and passes after**.

### 4b — Add a feature
Add a query `value-by-status` that reports net value grouped by trade status,
after events are applied. It must reuse the existing status rules. Touches
parsing/state/aggregation, the query layer, the CLI, and tests.

### 4c — Make it scale
The query rebuilds all state from scratch on every call. Make it handle a much
larger event stream without re-replaying everything per query. Explain the
before/after complexity in `SCALE.md`. Do not benchmark hardware — reason about
the algorithm.

## You must use — and submit — AI skills and agents
This is an applied-AI exercise. Drive the work with **Copilot customizations you
author**, and submit them as artifacts:

- **≥ 1 skill** — `phase4/.github/skills/<name>/SKILL.md`
- **≥ 1 custom agent** — `phase4/.github/agents/<name>.agent.md`
- optional reusable prompt(s) — `phase4/.github/prompts/<name>.prompt.md`

Example stubs are provided under `phase4/.github/`, each marked
**"EXAMPLE ONLY — replace with your own."** Submitting the stubs unchanged earns
nothing — author your own for the fix / feature / scale work.

## Deliverables (under `phase4/`)
- Your bug-fix and feature code, plus tests.
- The skill(s) and agent(s) you authored (see above).
- `WORKFLOW.md` (max one page): the plan you formed **before** prompting; how you
  split the three tasks; a **map of which skill/agent did which task**; and one
  place the AI was wrong and how you caught it.
- `SCALE.md` (a few lines): what was slow, what you changed, before/after
  complexity.

## What we score
Engineering judgment and control of the AI — not typing speed or how polished one
prompt sounds. You should be able to explain **every line** you submit.

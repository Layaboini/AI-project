# WORKFLOW.md

## Plan formed before prompting
1. Read `README.md` and every file on the data path (loader, state builder, calculator, query, CLI)
   and note which symptoms could span files.
2. Do the checkpoints in order, one commit each: **4a** fix, **4b** feature, **4c** scale.
3. Test first for each. 4a needs a test that fails before the fix. 4b must reuse the existing status
   rules. 4c must be explained by complexity and proven by an observable counter, not a benchmark.
4. Author the skill and agents as the written form of that process, and delete the example stubs.

## How the work was split
- **4a (bug):** `data/events/batch-a.json` and `batch-b.json` both contain event `F1`. The loader just
  concatenates files, and `StateBuilder` had no idempotency, so the fill ran twice (T1 quantity 5
  instead of 3, A001 = 300 instead of 200). Two failing tests plus a control test were written first;
  the fix is "apply each `event_id` once, in replay order".
- **4b (feature):** `ValueByStatusQuery` plus a `value-by-status` CLI command. It calls
  `RevenueCalculator.Counts`/`NetValue`, so cancelled trades are excluded and busted ones are negative.
- **4c (scale):** `TradeLedger` replays once, applies in-order events in O(1), and rebuilds lazily only
  after out-of-order input. See `SCALE.md`.

## Map: which skill/agent did which task
| Task | Skill / agent | How it was used |
|---|---|---|
| 4a bug fix | `engine-change` skill (steps 1-5), `engine-engineer` agent, `fix-bug` prompt | read path, hand-computed expected value, failing test first |
| 4b feature | `engine-change` skill (step 4: reuse `RevenueCalculator`) | no second copy of status rules |
| 4c scale | `engine-change` + `scale-review.md`, `engine-engineer` agent | complexity reasoning, `RebuildCount` test |
| Final check | `engine-reviewer` agent definition | review checklist applied by hand to the diff |

Honest note: the assistant used here was Claude Code, not Copilot, so these files were written to the
Copilot customization format and followed as a procedure; they were not executed by Copilot itself.

## One place the AI was wrong and how it was caught
While writing the Track A Phase 2 test, the AI assumed `US` sorts before `UNKNOWN` and indexed the
rows that way. `mvn package` failed (`expected UNKNOWN but was US`); the worked example
(`examples/phase-2-output.csv`) confirmed ordinal order puts `UNKNOWN` first, so the test was fixed, not
the code. A related slip: the first Phase 1 commit accidentally included Maven `target/` output; `git
status` before pushing showed it, and the commit was amended with a corrected `.gitignore`.

# Submission notes

## Where I stopped
All four phases are complete; nothing was left half-finished.

| Phase | Status | Where |
|---|---|---|
| 1 Proceeds per account | Done, matches the worked example | `track-a`, `out/phase1.json` |
| 2 Monthly value by region | Done, matches the worked example | `track-a`, `out/phase2.csv` |
| 3 Event-driven service | Done, matches the worked example | `track-a`, `DESIGN.md`, `out/phase3-sample.json` |
| 4a Bug fix | Done (event applied twice when redelivered across batch files) | `phase4/src/Domain` |
| 4b `value-by-status` | Done | `phase4/src/Query/ValueByStatusQuery.cs` |
| 4c Scale | Done (`TradeLedger`), reasoning in `phase4/SCALE.md` | `phase4/src/Domain/TradeLedger.cs` |

Track A is Java 17 + Maven. Phase 4 stays in C# because the provided engine is C#.

## Verification
- Java: `mvn -f track-a package` runs 52 tests, including the three worked examples and
  command-line tests for exit codes and error messages.
- C#: `dotnet test` in `phase4/` runs 32 tests, including the failing-first regression tests for 4a.
- `out/phase3-sample.json` was produced with:
  `java -jar track-a/target/trade-proceeds.jar top-accounts --start 2024-01-01 --end 2024-12-31 --limit 10 --offset 0`

## Known limits
- Phase 4 queries still scan every trade in current state (O(S)). The expensive replay is gone,
  but per-date or per-account indexes were not added (see `phase4/SCALE.md`).
- Event semantics that the task left open (replace vs merge on `new`, invalid events ignored whole)
  are recorded in `DECISIONS.md` and `DESIGN.md`.
- The Phase 4 skill and agent files follow the Copilot customization format, but the work was done
  with a different assistant and followed as a procedure (see `phase4/WORKFLOW.md`).
- `prompts/prompt-history.md` is hash-chained and verifiable, but entries are summaries and the first
  entry has identifying details redacted.

## Chat transcript
The exported chat session is in [`out/chat-transcript.zip`](out/chat-transcript.zip) (conversation
transcript plus session metadata). Personal and employer identifiers were replaced with placeholders
before it was added; the conversation content is otherwise unchanged.

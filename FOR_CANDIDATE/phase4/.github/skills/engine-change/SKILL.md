---
name: engine-change
description: Repeatable process for changing the TradeEngine C# code (bug fix, new query, or scaling work) safely. Use whenever a change touches StateBuilder, TradeLedger, the query classes or Program.cs.
---

# Engine change protocol

## When to use
Any change under `phase4/src`. It is a process, not an answer: it does not say what the fix is.

## Steps
1. **Read first.** Read `README.md`, then follow the data path end to end:
   `EventLoader` -> `StateBuilder`/`TradeLedger` -> `RevenueCalculator` -> a query -> `Program`.
   Note which files a symptom could span before touching any of them.
2. **Reproduce with data, not guesses.** Run the CLI on `data/` and compute the expected number by
   hand from the CSV and JSON. A bug is only confirmed when expected != actual.
3. **Test first.** Add a test that fails for the right reason (run it and read the failure), plus a
   control test that passes before and after so the fix cannot be over-broad.
4. **Smallest change.** Reuse existing rules (`RevenueCalculator.NetValue`/`Counts`); never copy a
   status rule into a new place.
5. **Run `dotnet test` and the CLI again.** Compare the CLI output with the hand-computed value.
6. **One concern per commit.** Fix, feature and scaling work are separate commits.
7. **Explain every line.** If a line cannot be justified in one sentence, remove or rewrite it.

## Checks that must pass
- `dotnet build` has no warnings introduced by the change.
- `dotnet test` is green, including the new failing-first test.
- Replay semantics are unchanged: ordering by (timestamp, event_id), each event_id applied once.

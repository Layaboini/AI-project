# SCALE.md

Notation: B = base trades, E = events, S = trades in current state (S <= B + E), A = accounts.

## What was slow
Every `TopAccountsQuery.Run` called `StateBuilder.Build`, which cloned all B trades, sorted all E
events (O(E log E)) and replayed them. A workload of Q queries over an event stream of size E
therefore cost **Q x (B + E log E)** before it even started aggregating, and new events could only
be picked up by repeating the whole thing.

## What changed
- New `TradeLedger` holds the current state and the events seen so far. Replay is paid once.
- `Ingest(event)` is O(1) when the event is at or after the last applied one (the normal live case):
  it is applied directly to the cached state.
- An out-of-order event (or an earlier redelivery of a known `event_id`) only marks the state stale.
  One lazy rebuild, O(E log E), happens on the next read, no matter how many late events arrived.
- Reads of unchanged state return the cached dictionary in O(1).
- Idempotency is a dictionary lookup by `event_id` (O(1)) instead of a pass over the sorted list.
- `TopAccountsQuery`/`ValueByStatusQuery` accept a shared ledger; their old constructors still work
  and replay once at construction instead of once per `Run`.
- `StateBuilder.Build` now delegates to the ledger so ordering/idempotency has one implementation.

## Before / after
| Operation | Before | After |
|---|---|---|
| Load E events, then Q queries | Q x O(B + E log E) | O(B + E log E) once, then Q x O(S + A log A) |
| One new in-order event, then a query | O(B + E log E) | O(1) ingest + O(S + A log A) |
| One late (out-of-order) event, then a query | O(B + E log E) | one O(E log E) rebuild, then normal query |

The query itself is still O(S + A log A): it scans current trades. That is intentional; getting
below that would need per-date indexes or pre-aggregated accounts, which is a bigger change and not
needed to remove the replay cost the task is about.

Tests: `TradeLedgerTests` assert via `RebuildCount` that repeated queries and in-order ingestion
never trigger a replay, and that out-of-order input matches a full replay.

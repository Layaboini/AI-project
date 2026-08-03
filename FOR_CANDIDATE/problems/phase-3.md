# Phase 3 — Event-driven trade service

This phase **extends Phase 2**. Reuse your parsing and net-value rules; add an
event log, a rebuilt current state, and a queryable interface.

## Input
- `data/trades.csv` — the base/initial state (apply Phase 1 malformed-row rules).
- `data/events/*.json` — one or more files, each a JSON array of events that
  change trade state over time. Event shape:

```json
{ "event_id": "E10", "type": "new", "timestamp": "2024-05-01T09:00:00Z",
  "trade_id": "T3001", "account_id": "A001", "quantity": 2, "price": 50.0,
  "status": "filled", "trade_date": "2024-05-01" }
```

`type` is one of:
- `new` — add (or replace) a trade with the fields given.
- `amend` — set any provided fields (`quantity`, `price`, `status`,
  `trade_date`) on an existing trade. If the `trade_id` does not exist, ignore.
- `cancel` — set the trade's `status` to `cancelled`.
- `bust` — set the trade's `status` to `busted`.

## Task
1. Load the base trades, then apply **all** events from **all** files.
2. Events must be applied in `timestamp` order (ascending), then `event_id`
   ascending. File names do **not** reflect order — sort by timestamp yourself.
3. Idempotency: if the same `event_id` appears more than once, apply it only the
   first time and ignore later duplicates.
4. After building current state, expose a query (CLI command or HTTP endpoint —
   your choice) named `top-accounts` with parameters:
   - `start` and `end` — inclusive `YYYY-MM-DD` date bounds on `trade_date`.
   - `limit` — max number of accounts to return (positive integer).
   - `offset` — number of leading accounts to skip (≥ 0).
5. The query computes net value per account (Phase 2 status rules) over trades
   whose `trade_date` is within `[start, end]`, ranks by `net_value` descending
   then `account_id` ascending, and returns the `limit` slice after `offset`.
   Each result element:

```json
{ "account_id": "A001", "net_value": 150.0, "trade_count": 1 }
```

6. Validate input: reject malformed dates, `start > end`, `limit <= 0`, or
   `offset < 0` with a clear error message (do not crash).

## Deliverables
- The service/CLI and **automated tests**.
- A short `DESIGN.md` (max one page) explaining your state model and how you
  guarantee correct ordering and idempotency.

`net_value` in JSON output is a **JSON number** (`150` and `150.0` are equal).

## Intentionally open
Some details (e.g. whether `new` on an existing `trade_id` fully replaces or
merges fields) are left to your judgement. State the assumption you took in
`DESIGN.md` / `DECISIONS.md`.

## Worked example
Base `examples/phase-3-base-trades.csv` plus events `examples/phase-3-events.json`,
queried with `start=2024-05-01 end=2024-05-31 limit=10 offset=0`, must produce
exactly `examples/phase-3-query-output.json`.

# Phase 3 design: event-driven trade service

## State model
- **Base state**: valid rows of `data/trades.csv` (Phase 1 malformed-row rules), keyed by `trade_id`
  in a `LinkedHashMap<String, Trade>`. Trades are copied so replays never mutate the loaded base.
- **Events** (`data/events/*.json`, all files merged) are applied over the base to produce *current
  state*. State is derived data: it is rebuilt from base + events, never persisted.
- **Query** (`top-accounts`) is a pure function over current state: filter by inclusive
  `trade_date` range, drop cancelled, sum net value per account (Phase 2 status rules), rank by
  net value descending then `account_id` ascending, then `skip(offset).limit(limit)`.

## Ordering guarantee
Events from every file are pooled, then sorted by `(timestamp as java.time.Instant, event_id)`.
The timestamp is parsed, not compared as text, so offsets and fractional seconds order correctly.
File names and in-file order play no role, so any permutation of the input gives the same state
(covered by a test that reverses the input).

## Idempotency guarantee
After sorting, a `HashSet<event_id>` is consulted before each apply; only the first occurrence of an
`event_id` is applied. Because the sort is total and deterministic, "first" is well defined even when
duplicates carry different timestamps (the earliest one wins). Re-running the whole pipeline on the
same inputs is also idempotent since state is rebuilt from scratch.

## Event semantics (assumptions)
- `new`: **full replace** of any existing trade with that `trade_id`; ignored if required fields are
  missing or invalid (unknown status, bad date, negative quantity or price).
- `amend`: sets only the provided fields; ignored for an unknown `trade_id`, and ignored as a whole
  if any provided value is invalid (no partial application).
- `cancel` / `bust`: set status; ignored for an unknown `trade_id`.
- Events with an unparseable timestamp, or missing `event_id`/`type`/`trade_id`, are skipped.
- An amend that arrives *before* the `new` it refers to (by timestamp) is ignored, as specified.

## Validation
`start`/`end` must be real `YYYY-MM-DD` dates, `start <= end`, `limit > 0`, `offset >= 0`. Violations
throw `InvalidQueryException`; the CLI prints `error: <message>` to stderr and exits 1.

## Complexity
Rebuild is O(E log E + B) for E events and B base trades; each query is O(S + A log A).

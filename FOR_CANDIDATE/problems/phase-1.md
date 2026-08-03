# Phase 1 — Proceeds per account

Build the foundation of a trade-proceeds tool. Later phases extend this code, so
keep it clean and reusable.

## Input
`data/trades.csv` with columns:

```
trade_id,account_id,quantity,price,status,trade_date
```

- `quantity` is an integer (shares), `price` is a decimal.
- `status` is one of: `filled`, `partially_filled`, `busted`, `cancelled`.
- `trade_date` is `YYYY-MM-DD`.

Some rows are not clean. Part of the task is deciding what counts as a usable
row and how to handle the rest.

## Task
Read `data/trades.csv` and produce, for each account, its proceeds from
**filled** trades only.

Rules:
1. Skip malformed rows. Print the number of skipped rows to standard error.
2. A trade's notional is `quantity * price`.
3. Only rows with `status == "filled"` count. Ignore all other statuses.
4. For each account, `trade_count` is the number of filled trades, and
   `proceeds` is the sum of their notionals, rounded to 2 decimals.
5. Accounts with no filled trades must not appear.

## Output
Write `out/phase1.json`: a JSON array, sorted by `proceeds` descending, then by
`account_id` ascending. Each element:

```json
{ "account_id": "A001", "trade_count": 2, "proceeds": 60.0 }
```

`proceeds` is a **JSON number** — `60` and `60.0` are equal and both accepted.

## Intentionally open
Some details (e.g. exactly what makes a row "malformed", how you parameterize the
input path) are left to your judgement. State the assumption you took in
`DECISIONS.md`.

## Worked example
Running your program on `examples/phase-1-input.csv` must produce exactly the
contents of `examples/phase-1-output.json` (and report `1` skipped row).

## Next
Phase 2 reuses this CSV parsing and notional logic — do not throw it away.
See `problems/phase-2.md`.

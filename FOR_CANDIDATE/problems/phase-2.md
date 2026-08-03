# Phase 2 — Monthly traded value by region

This phase **extends your Phase 1 program**. Reuse the same CSV parser and
notional logic; add account enrichment, busts, and grouping.

## Input
- `data/trades.csv` — same file and rules as Phase 1.
- `data/accounts.csv` with columns:

```
account_id,name,region,open_date
```

## Task
Produce a monthly traded-value report grouped by region and month.

Rules (these supersede Phase 1's counting):
1. Skip malformed trade rows, same as Phase 1.
2. Net contribution of a trade by status:
   - `filled` or `partially_filled` → `+ (quantity * price)`
   - `busted` → `- (quantity * price)`
   - `cancelled` → excluded entirely (does not count).
3. `month` is the first 7 characters of `trade_date` (`YYYY-MM`).
4. `region` comes from `data/accounts.csv`. If a trade's `account_id` is not in
   that file, use `"UNKNOWN"`.
5. Group by `(region, month)`:
   - `trade_count` = number of contributing trades (everything except cancelled
     and malformed).
   - `net_value` = sum of net contributions, rounded to 2 decimals.

## Output
Write `out/phase2.csv` with header and rows:

```
region,month,trade_count,net_value
```

Sort by `region` ascending, then `month` ascending. Format `net_value` with
exactly 2 decimal places (it may be negative).

## Intentionally open
Some details (e.g. how to handle a malformed row in `accounts.csv`) are left to
your judgement. State the assumption you took in `DECISIONS.md`.

## Worked example
Running on `examples/phase-2-input.csv` with `examples/phase-2-accounts.csv`
must produce exactly `examples/phase-2-output.csv`.

## Next
Phase 3 turns this batch report into a live, event-driven service built on top
of this aggregation logic. See `problems/phase-3.md`.

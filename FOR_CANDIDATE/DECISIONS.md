# Decisions and assumptions

## Language and layout
- Track A is Java 17 + Maven (`track-a/`). Phase 4 stays C# because the provided engine is C#.
- Run all commands from this directory (`FOR_CANDIDATE/`) so `data/` and `out/` resolve.
  Build: `mvn -f track-a package`; run: `java -jar track-a/target/trade-proceeds.jar phase1`.
- Money uses `BigDecimal` (no floating point drift); output rounded HALF_UP to 2 decimals.

## Phase 1
- Input path and output path are CLI options: `--trades` (default `data/trades.csv`), `--out`
  (default `out/phase1.json`).
- A row is **malformed** (skipped and counted) if any of: column count != 6; empty `trade_id` or
  `account_id`; `quantity` not an integer or negative; `price` not a decimal or negative;
  `status` not one of the four known values; `trade_date` not a real `YYYY-MM-DD` date.
- Unknown statuses (e.g. `settled`) are treated as malformed rather than silently ignored, so they
  show up in the skipped count.
- The header row and blank lines are not data rows and are not counted as skipped.
- `proceeds` is written as a JSON number with two decimals (`60.00`, equal to `60`).
- Skipped count is printed to stderr as a bare integer.

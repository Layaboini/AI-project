# TradeEngine

A small C# engine that builds current trade state from a base blotter plus an
event stream, and answers ranking queries over it.

## Run
```
dotnet build
dotnet test
dotnet run --project src -- top-accounts --start 2024-05-01 --end 2024-05-31 --limit 5 --offset 0
```
Run commands from this `phase4/` directory so the default `data/` path resolves.
Override with `--data <dir>` if needed.

## Layout
```
src/
  Program.cs               CLI entry + argument validation
  Models/Trade.cs          Trade state
  Models/TradeEvent.cs     Event shape (JSON)
  Parsing/TradeCsvParser   Base blotter loader
  Parsing/EventLoader      Loads events from every JSON file in a dir
  Domain/StateBuilder      Replays events over the blotter
  Domain/RevenueCalculator Net-value rules by status
  Query/TopAccountsQuery   Ranks accounts by net value in a date window
tests/                     xUnit tests
data/                      Sample blotter + event batches
.github/                   Example skill/agent/prompt stubs (replace with yours)
```

## Data model
- Base trades: `trade_id,account_id,quantity,price,status,trade_date`.
- Events (`data/events/*.json`): `new`, `fill` (adds quantity), `cancel`, `bust`.
  Each has an `event_id` and a `timestamp`; events apply in timestamp order.
- Net value: `filled` / `partially_filled` add `quantity * price`; `busted`
  subtracts it; `cancelled` is excluded.

See `../problems/phase-4.md` for what to do with this code.

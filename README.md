# AI-project: Trade Proceeds

A small trade-processing project built in two tracks. All work lives in [`FOR_CANDIDATE/`](FOR_CANDIDATE/).

| Track | Language | Location | What it does |
|---|---|---|---|
| A, phases 1-3 | Java 17 + Maven | [`FOR_CANDIDATE/track-a`](FOR_CANDIDATE/track-a) | Proceeds per account, monthly net value by region, event-driven `top-accounts` query |
| B, phase 4 | C# (.NET 8) | [`FOR_CANDIDATE/phase4`](FOR_CANDIDATE/phase4) | Bug fix, `value-by-status` query, incremental state for scale |

## Where to read first
- [`FOR_CANDIDATE/DECISIONS.md`](FOR_CANDIDATE/DECISIONS.md): assumptions for the open requirements.
- [`FOR_CANDIDATE/DESIGN.md`](FOR_CANDIDATE/DESIGN.md): Phase 3 state model, ordering and idempotency.
- [`FOR_CANDIDATE/phase4/WORKFLOW.md`](FOR_CANDIDATE/phase4/WORKFLOW.md) and [`SCALE.md`](FOR_CANDIDATE/phase4/SCALE.md): Phase 4 process and complexity notes.
- [`FOR_CANDIDATE/problems/`](FOR_CANDIDATE/problems): the task statements.

## Run Track A (Java)
Needs JDK 17 and Maven. Run from `FOR_CANDIDATE/` so the default `data/` and `out/` paths resolve.

```
cd FOR_CANDIDATE
mvn -f track-a package
java -jar track-a/target/trade-proceeds.jar phase1
java -jar track-a/target/trade-proceeds.jar phase2
java -jar track-a/target/trade-proceeds.jar top-accounts --start 2024-05-01 --end 2024-05-31 --limit 5 --offset 0
```

Outputs are written to `FOR_CANDIDATE/out/`. `mvn package` also runs the tests, which check the
worked examples in `FOR_CANDIDATE/examples/`.

## Run Phase 4 (C#)
Needs the .NET SDK (8 or newer).

```
cd FOR_CANDIDATE/phase4
dotnet test
dotnet run --project src -- top-accounts --start 2024-05-01 --end 2024-05-31 --limit 5 --offset 0
dotnet run --project src -- value-by-status
```

## Commit history
History is split into one commit per chunk: gitignore, phase 1, phase 2, phase 3, phase 4a (fix),
4b (feature), 4c (scale), then the Phase 4 skill/agent files and workflow notes.

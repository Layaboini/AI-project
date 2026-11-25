---
agent: engine-engineer
description: Reproduce, test-first fix and verify a suspected double-counting or state bug in TradeEngine.
---

Symptom: ${input:symptom:describe the wrong number or behaviour}

Follow the engine-change skill. First compute the expected result by hand from `data/` and show it
next to the CLI output. Then write a failing test and a control test, fix it with the smallest change,
and report the test and CLI results.

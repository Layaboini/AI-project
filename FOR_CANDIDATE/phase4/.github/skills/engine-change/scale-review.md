# Scale review checklist (used by the scale step of engine-change)

- What is the per-query cost today, in terms of B (base trades), E (events), S (state size)?
- Which work is repeated per call but depends only on data that did not change?
- What is the *live* case (events arriving in order) versus the rare case (late events)?
  Make the live case O(1) and make the rare case correct, not necessarily fast.
- State the before/after complexity in `SCALE.md` and add a test that observes the behaviour
  (for example a rebuild counter) instead of timing hardware.

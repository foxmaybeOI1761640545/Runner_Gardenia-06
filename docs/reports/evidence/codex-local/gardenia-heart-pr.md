Runner Gardenia already records and charts BPM, but its BLE lifecycle and optional SIG measurement handling need improvement. This draft develops the changes in validated stages while preserving the existing sensor/recording/chart architecture and local-only data.

Stage 0 documents the actual baseline, reference-module boundaries, gaps, planned changes and acceptance checks. Later stages will add protocol/lifecycle, recording metadata, integrated UI/screenshots, regression coverage, sign-before-tag publication and a persisted version ledger before authorized orphan cleanup.

Intermediate commits remain on this feature branch and run PR CI only. Final merge to main is deferred until every stage passes; hardware-only checks will be explicitly recorded.

Validation: baseline pipeline-helper regressions; audit scope checks; git diff --check. Full PR CI is required before beginning stage 1.

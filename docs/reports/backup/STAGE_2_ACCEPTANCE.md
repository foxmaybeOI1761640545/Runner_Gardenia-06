# Stage 2: fail-closed inventory verification

Local checks passed: 29 new real-file/CLI tests; full helper suite 100 tests, 0 failures/errors/skips. Normal inventory:17/17 passed. Negative tests include same-size byte tampering (nonzero CLI exit), missing files, duplicate paths/JSON keys, empty inventory, Windows/absolute/traversal paths and symlink escapes. YAML parsed and existing actionlint passed. Historical archive verifier still confirms66 manifests/1759 original members/1693 unique objects and unchanged protected refs.

Tool installation commit: `502b37d` (`[skip ci]` prevents prohibited Android push builds). First new-workflow dispatch failed with GitHub HTTP404 because the branch-only workflow was not registered. This failed attempt is retained, not reported as passed. Recovery: this normal documentation-only push should register and run the backup workflow without matching the unmodified Android workflow. Cloud result and exact remote checks will be recorded in the final report after execution. No gate disabled.

Stage1 cloud gate: [42359725754](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42359725754), source31bdff76d90722cf59142fff95fd7349f3235897, success; downloaded job log confirms raw archive counts. All18 changed remote Blobs matched local bytes.

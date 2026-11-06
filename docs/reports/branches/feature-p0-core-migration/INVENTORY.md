# Historical evidence inventory

Snapshot: 2026-11-06T07:14:41.765170+00:00. Archive baseline `60daeb21aa50bdbceec6e6ad354a431b8c1be434`.

A = exact blob verified on remote Git; B = local committed, not on remote; C = untracked working file; D = Codex-only; E = recovered from exact Git object/Actions Artifact; F = unavailable/not found. A filename match is never treated as a byte match. New archive files are E/C until the archive commit is pushed and remotely verified. Receipt records the transition to A.

## Scan coverage

- Whole tracked tree, untracked list, all reachable Git history and all three remote branch trees scanned; initial working tree clean (no B/C user changes).
- `doc/` and `.forgejo/workflows/` absent. `.gitea/` contains upstream issue/PR templates, not acceptance evidence. Both GitHub workflows retained.
- `screenshots-svg/`, fastlane screenshots and application resource trees are upstream product/marketing assets; no relocation or false acceptance classification.
- 66 downloadable historical report Artifacts recovered, including failed/cancelled runs; all downloaded archive digests checked against GitHub metadata. No Artifact recovery failures.
- Existing docs/ and 12 heart-rate PNGs remain at original paths. Exact-byte objects deduplicate recovered artifacts; every original member path is retained in its artifact manifest. Logs/HTML/emulator payloads not selected are explicitly listed in `notArchived` and remain at the referenced Artifact.
- No APK, database, key, build cache or signing files added. GPX files are synthetic public CI fixtures, not personal workouts.
- No `HEART_RATE_BLE_IMPLEMENTATION.md` was found in any reachable tree/history or workspace. Existing architecture source is `HEART_RATE_IMPLEMENTATION_AUDIT.md`; record F (not found), do not fabricate a replacement historical report.

## Original repository files

| File path | Category | Version/branch | Git state | Remote state | Handling |
|---|---|---|---|---|---|
| [.forgejo/workflows/test.yaml](../../../../.forgejo/workflows/test.yaml) | CI configuration | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 0fc824c8c64e | preserved original path/bytes |
| [.gitea/ISSUE_TEMPLATE/bug_report.md](../../../../.gitea/ISSUE_TEMPLATE/bug_report.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 5027a6dac708 | preserved original path/bytes |
| [.gitea/ISSUE_TEMPLATE/data_not_recorded.md](../../../../.gitea/ISSUE_TEMPLATE/data_not_recorded.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 94a4136fd92b | preserved original path/bytes |
| [.gitea/ISSUE_TEMPLATE/feature_request.md](../../../../.gitea/ISSUE_TEMPLATE/feature_request.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 64517387de14 | preserved original path/bytes |
| [.gitea/PULL_REQUEST_TEMPLATE.md](../../../../.gitea/PULL_REQUEST_TEMPLATE.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 8cd403239d5d | preserved original path/bytes |
| [.github/workflows/android-build.yml](../../../../.github/workflows/android-build.yml) | CI configuration | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob c7742212b04e | preserved original path/bytes |
| [.github/workflows/p0-core.yml](../../../../.github/workflows/p0-core.yml) | CI configuration | feature/p0-core-migration | A | exact blob 99f34dedcf9b | preserved original path/bytes |
| [AGENTS.md](../../../../AGENTS.md) | build/development instructions | feature/p0-core-migration | A | exact blob 5ddbcd8bbea7 | preserved original path/bytes |
| [BUILDING_P0.md](../../../../BUILDING_P0.md) | build/development instructions | feature/p0-core-migration | A | exact blob 56317c25d06f | preserved original path/bytes |
| [BUILDING_RUNNER_GARDENIA.md](../../../../BUILDING_RUNNER_GARDENIA.md) | build/development instructions | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 48473736dfe7 | preserved original path/bytes |
| [CONTRIBUTING.md](../../../../CONTRIBUTING.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob c676a1e95697 | preserved original path/bytes |
| [README.md](../../../../README.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob d06dd4b7b781 | preserved original path/bytes |
| [README_API.md](../../../../README_API.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 96b1d2c58f63 | preserved original path/bytes |
| [README_RELEASE.md](../../../../README_RELEASE.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 24b3059930f4 | preserved original path/bytes |
| [README_TESTED_SENSORS.md](../../../../README_TESTED_SENSORS.md) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 307c4492f7c0 | preserved original path/bytes |
| [doc/activities.txt](../../../../doc/activities.txt) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob effa3e71c8f7 | preserved original path/bytes |
| [doc/data-exportimport.txt](../../../../doc/data-exportimport.txt) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob c8310c7ff54b | preserved original path/bytes |
| [doc/data-flow.txt](../../../../doc/data-flow.txt) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob d0e570a30c9c | preserved original path/bytes |
| [doc/data-structure.txt](../../../../doc/data-structure.txt) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 73c8d587d7cc | preserved original path/bytes |
| [doc/xml/gpx.xsd](../../../../doc/xml/gpx.xsd) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob a113a41c30c9 | preserved original path/bytes |
| [doc/xml/ogckml23.xsd](../../../../doc/xml/ogckml23.xsd) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob e6e825cbef1a | preserved original path/bytes |
| [doc/xml/opentracks-schema-1.0.xsd](../../../../doc/xml/opentracks-schema-1.0.xsd) | upstream documentation/template/resource | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 89b187fb1423 | preserved original path/bytes |
| [docs/HEART_RATE_BLE_TEST_CHECKLIST.md](../../../../docs/HEART_RATE_BLE_TEST_CHECKLIST.md) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob d678d541801c | preserved original path/bytes |
| [docs/HEART_RATE_IMPLEMENTATION_AUDIT.md](../../../../docs/HEART_RATE_IMPLEMENTATION_AUDIT.md) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 631d8390c8dc | preserved original path/bytes |
| [docs/HEART_RATE_STAGE_ACCEPTANCE.md](../../../../docs/HEART_RATE_STAGE_ACCEPTANCE.md) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob c88052347fc9 | preserved original path/bytes |
| [docs/HEART_RATE_UI_REVIEW.md](../../../../docs/HEART_RATE_UI_REVIEW.md) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob f744084600ce | preserved original path/bytes |
| [docs/P0_ACCEPTANCE.md](../../../../docs/P0_ACCEPTANCE.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob ad468781089e | preserved original path/bytes |
| [docs/P0_BASELINE.json](../../../../docs/P0_BASELINE.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 976c5fe521f8 | preserved original path/bytes |
| [docs/P0_DELIVERY_REPORT.md](../../../../docs/P0_DELIVERY_REPORT.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 4ccde07ff8cf | preserved original path/bytes |
| [docs/P0_DEVICE_CHECKLIST.md](../../../../docs/P0_DEVICE_CHECKLIST.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 3ec60a40827d | preserved original path/bytes |
| [docs/P0_FINAL_TECHNICAL_REVIEW.md](../../../../docs/P0_FINAL_TECHNICAL_REVIEW.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 0166b9a83cc3 | preserved original path/bytes |
| [docs/P0_GPX_FORMAT.md](../../../../docs/P0_GPX_FORMAT.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 6b8080860799 | preserved original path/bytes |
| [docs/P0_MIGRATION_PLAN.md](../../../../docs/P0_MIGRATION_PLAN.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 275222d11e65 | preserved original path/bytes |
| [docs/P0_OFFLINE_AUDIT.md](../../../../docs/P0_OFFLINE_AUDIT.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 167d1343a9cd | preserved original path/bytes |
| [docs/P0_PUBLICATION_AND_REVIEW.md](../../../../docs/P0_PUBLICATION_AND_REVIEW.md) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob cfc135178670 | preserved original path/bytes |
| [docs/RELEASE_TAG_AUDIT.md](../../../../docs/RELEASE_TAG_AUDIT.md) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob c6134725d083 | preserved original path/bytes |
| [docs/p0-evidence/p0-final-review.json](../../../../docs/p0-evidence/p0-final-review.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 8500d79e5f6e | preserved original path/bytes |
| [docs/p0-evidence/stage-1.json](../../../../docs/p0-evidence/stage-1.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob ed8a2885a301 | preserved original path/bytes |
| [docs/p0-evidence/stage-10.json](../../../../docs/p0-evidence/stage-10.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 24ea3a441d7f | preserved original path/bytes |
| [docs/p0-evidence/stage-2.json](../../../../docs/p0-evidence/stage-2.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 0008f6d79eb9 | preserved original path/bytes |
| [docs/p0-evidence/stage-3.json](../../../../docs/p0-evidence/stage-3.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob f2f4c317461c | preserved original path/bytes |
| [docs/p0-evidence/stage-4.json](../../../../docs/p0-evidence/stage-4.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 00e54d4254e0 | preserved original path/bytes |
| [docs/p0-evidence/stage-5.json](../../../../docs/p0-evidence/stage-5.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob 54532a1ad7f4 | preserved original path/bytes |
| [docs/p0-evidence/stage-6.json](../../../../docs/p0-evidence/stage-6.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob b007f94ba8e6 | preserved original path/bytes |
| [docs/p0-evidence/stage-7.json](../../../../docs/p0-evidence/stage-7.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob c1fcace41d55 | preserved original path/bytes |
| [docs/p0-evidence/stage-8.json](../../../../docs/p0-evidence/stage-8.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob b9a3562db7c5 | preserved original path/bytes |
| [docs/p0-evidence/stage-9.json](../../../../docs/p0-evidence/stage-9.json) | Runner Gardenia report/evidence | feature/p0-core-migration | A | exact blob cd6165d97369 | preserved original path/bytes |
| [docs/screenshots/heart-rate/SHA256SUMS](../../../../docs/screenshots/heart-rate/SHA256SUMS) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 62e5f9d6f19f | preserved original path/bytes |
| [docs/screenshots/heart-rate/capture-manifest.json](../../../../docs/screenshots/heart-rate/capture-manifest.json) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 2f0ad358834d | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-dark-home.png](../../../../docs/screenshots/heart-rate/narrow-dark-home.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 7f54762a58a9 | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-dark-recording-connected.png](../../../../docs/screenshots/heart-rate/narrow-dark-recording-connected.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 828e6f6585fa | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-dark-settings.png](../../../../docs/screenshots/heart-rate/narrow-dark-settings.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 5eb1f34ccc3f | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-dark-time-chart.png](../../../../docs/screenshots/heart-rate/narrow-dark-time-chart.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 80f794a27e85 | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-light-history.png](../../../../docs/screenshots/heart-rate/narrow-light-history.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 0238cb276371 | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-light-recording-connected.png](../../../../docs/screenshots/heart-rate/narrow-light-recording-connected.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 07eb9003a72c | preserved original path/bytes |
| [docs/screenshots/heart-rate/narrow-light-scan-fixture.png](../../../../docs/screenshots/heart-rate/narrow-light-scan-fixture.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob cf217e0cdcb8 | preserved original path/bytes |
| [docs/screenshots/heart-rate/phone-dark-distance-chart.png](../../../../docs/screenshots/heart-rate/phone-dark-distance-chart.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 4c094aacc628 | preserved original path/bytes |
| [docs/screenshots/heart-rate/phone-dark-recording-stale.png](../../../../docs/screenshots/heart-rate/phone-dark-recording-stale.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 53cea2be8529 | preserved original path/bytes |
| [docs/screenshots/heart-rate/phone-dark-settings.png](../../../../docs/screenshots/heart-rate/phone-dark-settings.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob e0a10b2a2516 | preserved original path/bytes |
| [docs/screenshots/heart-rate/phone-light-recording-connected.png](../../../../docs/screenshots/heart-rate/phone-light-recording-connected.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob dc1ed13d1229 | preserved original path/bytes |
| [docs/screenshots/heart-rate/phone-light-time-chart.png](../../../../docs/screenshots/heart-rate/phone-light-time-chart.png) | Runner Gardenia report/evidence | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 97cf3147230d | preserved original path/bytes |
| [release-ledger.json](../../../../release-ledger.json) | build/development instructions | main, feature/p0-core-migration, feature/heart-rate-monitoring | A | exact blob 602ea443293c | preserved original path/bytes |

## Recovered original test reports and screenshots

Full per-file inventory (original path, byte count, SHA-256, canonical object path and XML counts) is provided by each manifest below. This is the complete recovered member list, not a filename-only summary. Every object remains original bytes.

| Artifact / run | Status | Files | Full inventory |
|---|---|---|---|
| [check-reports-41641699738-1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41641699738/artifacts/13232583546) | E → A after remote verification; failure | 0 | [manifest](../../evidence/artifacts/13232583546/manifest.json) |
| [check-reports-41642835327-1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41642835327/artifacts/13233572914) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13233572914/manifest.json) |
| [check-reports-41644927740-1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41644927740/artifacts/13234423775) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13234423775/manifest.json) |
| [check-reports-41647576789-1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41647576789/artifacts/13236455806) | E → A after remote verification; success | 4 | [manifest](../../evidence/artifacts/13236455806/manifest.json) |
| [check-reports-41671404110-1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41671404110/artifacts/13247299686) | E → A after remote verification; success | 4 | [manifest](../../evidence/artifacts/13247299686/manifest.json) |
| [check-reports-41672584767-1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41672584767/artifacts/13249960644) | E → A after remote verification; failure | 4 | [manifest](../../evidence/artifacts/13249960644/manifest.json) |
| [check-reports-41681169734](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41681169734/artifacts/13252923240) | E → A after remote verification; failure | 4 | [manifest](../../evidence/artifacts/13252923240/manifest.json) |
| [check-reports-41685585228](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41685585228/artifacts/13253724407) | E → A after remote verification; success | 4 | [manifest](../../evidence/artifacts/13253724407/manifest.json) |
| [check-reports-41683034188](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41683034188/artifacts/13253852187) | E → A after remote verification; failure | 4 | [manifest](../../evidence/artifacts/13253852187/manifest.json) |
| [check-reports-41691689014](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41691689014/artifacts/13257594253) | E → A after remote verification; success | 4 | [manifest](../../evidence/artifacts/13257594253/manifest.json) |
| [check-reports-41698875373](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41698875373/artifacts/13260959972) | E → A after remote verification; cancelled | 1 | [manifest](../../evidence/artifacts/13260959972/manifest.json) |
| [check-reports-41699182101](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41699182101/artifacts/13261638589) | E → A after remote verification; failure | 5 | [manifest](../../evidence/artifacts/13261638589/manifest.json) |
| [check-reports-41699651106](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41699651106/artifacts/13262668230) | E → A after remote verification; success | 7 | [manifest](../../evidence/artifacts/13262668230/manifest.json) |
| [check-reports-41701825550](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41701825550/artifacts/13263821730) | E → A after remote verification; success | 7 | [manifest](../../evidence/artifacts/13263821730/manifest.json) |
| [check-reports-41704991007](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41704991007/artifacts/13264093732) | E → A after remote verification; cancelled | 1 | [manifest](../../evidence/artifacts/13264093732/manifest.json) |
| [check-reports-41705391514](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41705391514/artifacts/13265179111) | E → A after remote verification; failure | 6 | [manifest](../../evidence/artifacts/13265179111/manifest.json) |
| [check-reports-41704278929](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41704278929/artifacts/13265581804) | E → A after remote verification; cancelled | 7 | [manifest](../../evidence/artifacts/13265581804/manifest.json) |
| [check-reports-41706341684](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41706341684/artifacts/13266100549) | E → A after remote verification; failure | 15 | [manifest](../../evidence/artifacts/13266100549/manifest.json) |
| [check-reports-41728310766](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41728310766/artifacts/13275534650) | E → A after remote verification; success | 49 | [manifest](../../evidence/artifacts/13275534650/manifest.json) |
| [check-reports-41727601648](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41727601648/artifacts/13276120197) | E → A after remote verification; failure | 6 | [manifest](../../evidence/artifacts/13276120197/manifest.json) |
| [check-reports-41730116406](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41730116406/artifacts/13276545732) | E → A after remote verification; failure | 6 | [manifest](../../evidence/artifacts/13276545732/manifest.json) |
| [check-reports-41732992884](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41732992884/artifacts/13278193212) | E → A after remote verification; cancelled | 7 | [manifest](../../evidence/artifacts/13278193212/manifest.json) |
| [check-reports-41730815364](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41730815364/artifacts/13278305913) | E → A after remote verification; success | 49 | [manifest](../../evidence/artifacts/13278305913/manifest.json) |
| [check-reports-41733224958](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41733224958/artifacts/13278496081) | E → A after remote verification; success | 51 | [manifest](../../evidence/artifacts/13278496081/manifest.json) |
| [check-reports-41737068350](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41737068350/artifacts/13278943667) | E → A after remote verification; success | 51 | [manifest](../../evidence/artifacts/13278943667/manifest.json) |
| [check-reports-41738430305](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41738430305/artifacts/13279853488) | E → A after remote verification; success | 51 | [manifest](../../evidence/artifacts/13279853488/manifest.json) |
| [check-reports-41735349725](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41735349725/artifacts/13279900203) | E → A after remote verification; success | 51 | [manifest](../../evidence/artifacts/13279900203/manifest.json) |
| [check-reports-41736907818](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41736907818/artifacts/13280235585) | E → A after remote verification; cancelled | 2 | [manifest](../../evidence/artifacts/13280235585/manifest.json) |
| [check-reports-41848524070](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41848524070/artifacts/13332512386) | E → A after remote verification; success | 51 | [manifest](../../evidence/artifacts/13332512386/manifest.json) |
| [P0-reports-41870121327](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41870121327/artifacts/13343253840) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13343253840/manifest.json) |
| [P0-reports-41870693111](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41870693111/artifacts/13344567669) | E → A after remote verification; success | 53 | [manifest](../../evidence/artifacts/13344567669/manifest.json) |
| [P0-reports-41873228190](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41873228190/artifacts/13344694259) | E → A after remote verification; failure | 12 | [manifest](../../evidence/artifacts/13344694259/manifest.json) |
| [P0-reports-41874166590](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41874166590/artifacts/13345348690) | E → A after remote verification; success | 13 | [manifest](../../evidence/artifacts/13345348690/manifest.json) |
| [P0-reports-41878171031](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41878171031/artifacts/13347802808) | E → A after remote verification; failure | 15 | [manifest](../../evidence/artifacts/13347802808/manifest.json) |
| [P0-reports-41880252503](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41880252503/artifacts/13348414922) | E → A after remote verification; cancelled | 13 | [manifest](../../evidence/artifacts/13348414922/manifest.json) |
| [P0-reports-41884537905](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41884537905/artifacts/13349999077) | E → A after remote verification; cancelled | 13 | [manifest](../../evidence/artifacts/13349999077/manifest.json) |
| [P0-reports-41883615254](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41883615254/artifacts/13350217915) | E → A after remote verification; failure | 15 | [manifest](../../evidence/artifacts/13350217915/manifest.json) |
| [P0-reports-41884722256](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41884722256/artifacts/13351591434) | E → A after remote verification; failure | 15 | [manifest](../../evidence/artifacts/13351591434/manifest.json) |
| [P0-reports-41886672130](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41886672130/artifacts/13353005045) | E → A after remote verification; success | 16 | [manifest](../../evidence/artifacts/13353005045/manifest.json) |
| [P0-reports-41891475431](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41891475431/artifacts/13353517717) | E → A after remote verification; success | 18 | [manifest](../../evidence/artifacts/13353517717/manifest.json) |
| [P0-reports-41889665084](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41889665084/artifacts/13353865172) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13353865172/manifest.json) |
| [P0-reports-41890292122](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41890292122/artifacts/13354960248) | E → A after remote verification; failure | 17 | [manifest](../../evidence/artifacts/13354960248/manifest.json) |
| [P0-reports-41895858575](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41895858575/artifacts/13356585659) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13356585659/manifest.json) |
| [P0-reports-41896259632](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41896259632/artifacts/13356596683) | E → A after remote verification; success | 21 | [manifest](../../evidence/artifacts/13356596683/manifest.json) |
| [P0-reports-41927892629](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41927892629/artifacts/13368934307) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13368934307/manifest.json) |
| [P0-reports-41928481409](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41928481409/artifacts/13369139525) | E → A after remote verification; cancelled | 2 | [manifest](../../evidence/artifacts/13369139525/manifest.json) |
| [P0-reports-41928763689](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41928763689/artifacts/13369718793) | E → A after remote verification; failure | 22 | [manifest](../../evidence/artifacts/13369718793/manifest.json) |
| [P0-reports-41929851364](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41929851364/artifacts/13370489833) | E → A after remote verification; success | 24 | [manifest](../../evidence/artifacts/13370489833/manifest.json) |
| [P0-reports-41928368419](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41928368419/artifacts/13370790662) | E → A after remote verification; cancelled | 2 | [manifest](../../evidence/artifacts/13370790662/manifest.json) |
| [P0-reports-41933012158](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41933012158/artifacts/13371489682) | E → A after remote verification; success | 34 | [manifest](../../evidence/artifacts/13371489682/manifest.json) |
| [P0-reports-41932103128](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41932103128/artifacts/13372128554) | E → A after remote verification; failure | 24 | [manifest](../../evidence/artifacts/13372128554/manifest.json) |
| [P0-reports-41935322993](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41935322993/artifacts/13374111460) | E → A after remote verification; failure | 34 | [manifest](../../evidence/artifacts/13374111460/manifest.json) |
| [P0-reports-41937243378](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41937243378/artifacts/13374664030) | E → A after remote verification; failure | 96 | [manifest](../../evidence/artifacts/13374664030/manifest.json) |
| [P0-reports-41938835493](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41938835493/artifacts/13374943779) | E → A after remote verification; success | 99 | [manifest](../../evidence/artifacts/13374943779/manifest.json) |
| [P0-reports-41936236159](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41936236159/artifacts/13375050677) | E → A after remote verification; success | 35 | [manifest](../../evidence/artifacts/13375050677/manifest.json) |
| [P0-reports-41943762519](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41943762519/artifacts/13377528578) | E → A after remote verification; success | 99 | [manifest](../../evidence/artifacts/13377528578/manifest.json) |
| [P0-reports-41947162401](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41947162401/artifacts/13379308335) | E → A after remote verification; failure | 97 | [manifest](../../evidence/artifacts/13379308335/manifest.json) |
| [P0-reports-41948538632](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41948538632/artifacts/13380137146) | E → A after remote verification; failure | 97 | [manifest](../../evidence/artifacts/13380137146/manifest.json) |
| [P0-Release-reports-41968727319](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41968727319/artifacts/13389073426) | E → A after remote verification; failure | 0 | [manifest](../../evidence/artifacts/13389073426/manifest.json) |
| [P0-reports-41968727319](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41968727319/artifacts/13389102578) | E → A after remote verification; failure | 99 | [manifest](../../evidence/artifacts/13389102578/manifest.json) |
| [P0-Release-reports-41970434767](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41970434767/artifacts/13389688053) | E → A after remote verification; failure | 4 | [manifest](../../evidence/artifacts/13389688053/manifest.json) |
| [P0-reports-41970434767](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41970434767/artifacts/13390005982) | E → A after remote verification; failure | 99 | [manifest](../../evidence/artifacts/13390005982/manifest.json) |
| [P0-Release-reports-41972826140](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41972826140/artifacts/13391017652) | E → A after remote verification; failure | 2 | [manifest](../../evidence/artifacts/13391017652/manifest.json) |
| [P0-reports-41972826140](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41972826140/artifacts/13391120749) | E → A after remote verification; failure | 99 | [manifest](../../evidence/artifacts/13391120749/manifest.json) |
| [P0-reports-41974364095](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391716581) | E → A after remote verification; success | 99 | [manifest](../../evidence/artifacts/13391716581/manifest.json) |
| [P0-Release-reports-41974364095](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391757704) | E → A after remote verification; success | 8 | [manifest](../../evidence/artifacts/13391757704/manifest.json) |

## Actual XML counts

Counts are parsed from original testcase elements; failed/cancelled/incomplete suites remain explicitly failed/incomplete. Separate durability process tests are not added again as new unique suite tests.

| Run | Artifact | Type | Tests | Executed | Failures | Errors | Skipped | Run conclusion |
|---|---|---|---|---|---|---|---|---|
| 41642835327 | 13233572914 | Android | 289 | 288 | 3 | 0 | 1 | failure |
| 41644927740 | 13234423775 | Android | 105 | 105 | 2 | 0 | 0 | failure |
| 41647576789 | 13236455806 | Android | 293 | 292 | 0 | 0 | 1 | success |
| 41671404110 | 13247299686 | Android | 293 | 292 | 0 | 0 | 1 | success |
| 41672584767 | 13249960644 | Android | 293 | 292 | 0 | 0 | 1 | failure |
| 41681169734 | 13252923240 | Android | 294 | 293 | 0 | 0 | 1 | failure |
| 41683034188 | 13253852187 | Android | 294 | 293 | 0 | 0 | 1 | failure |
| 41685585228 | 13253724407 | Android | 294 | 293 | 0 | 0 | 1 | success |
| 41691689014 | 13257594253 | Android | 294 | 293 | 0 | 0 | 1 | success |
| 41699182101 | 13261638589 | JVM | 38 | 38 | 0 | 0 | 0 | failure |
| 41699651106 | 13262668230 | Android | 304 | 303 | 0 | 0 | 1 | success |
| 41699651106 | 13262668230 | JVM | 38 | 38 | 0 | 0 | 0 | success |
| 41701825550 | 13263821730 | Android | 313 | 312 | 0 | 0 | 1 | success |
| 41701825550 | 13263821730 | JVM | 38 | 38 | 0 | 0 | 0 | success |
| 41704278929 | 13265581804 | Android | 317 | 316 | 0 | 0 | 1 | cancelled |
| 41704278929 | 13265581804 | JVM | 44 | 44 | 0 | 0 | 0 | cancelled |
| 41705391514 | 13265179111 | JVM | 44 | 44 | 0 | 0 | 0 | failure |
| 41706341684 | 13266100549 | Android | 325 | 324 | 1 | 0 | 1 | failure |
| 41706341684 | 13266100549 | JVM | 44 | 44 | 0 | 0 | 0 | failure |
| 41727601648 | 13276120197 | JVM | 45 | 45 | 0 | 0 | 0 | failure |
| 41728310766 | 13275534650 | Android | 326 | 325 | 0 | 0 | 1 | success |
| 41728310766 | 13275534650 | JVM | 45 | 45 | 0 | 0 | 0 | success |
| 41730116406 | 13276545732 | JVM | 45 | 45 | 0 | 0 | 0 | failure |
| 41730815364 | 13278305913 | Android | 326 | 325 | 0 | 0 | 1 | success |
| 41730815364 | 13278305913 | JVM | 45 | 45 | 0 | 0 | 0 | success |
| 41732992884 | 13278193212 | JVM | 51 | 51 | 0 | 0 | 0 | cancelled |
| 41733224958 | 13278496081 | Android | 328 | 327 | 0 | 0 | 1 | success |
| 41733224958 | 13278496081 | JVM | 52 | 52 | 0 | 0 | 0 | success |
| 41735349725 | 13279900203 | Android | 328 | 327 | 0 | 0 | 1 | success |
| 41735349725 | 13279900203 | JVM | 52 | 52 | 0 | 0 | 0 | success |
| 41737068350 | 13278943667 | Android | 328 | 327 | 0 | 0 | 1 | success |
| 41737068350 | 13278943667 | JVM | 52 | 52 | 0 | 0 | 0 | success |
| 41738430305 | 13279853488 | Android | 328 | 327 | 0 | 0 | 1 | success |
| 41738430305 | 13279853488 | JVM | 52 | 52 | 0 | 0 | 0 | success |
| 41848524070 | 13332512386 | Android | 328 | 327 | 0 | 0 | 1 | success |
| 41848524070 | 13332512386 | JVM | 52 | 52 | 0 | 0 | 0 | success |
| 41870693111 | 13344567669 | Android | 330 | 329 | 0 | 0 | 1 | success |
| 41870693111 | 13344567669 | JVM | 56 | 56 | 0 | 0 | 0 | success |
| 41873228190 | 13344694259 | Android | 24 | 24 | 0 | 0 | 0 | failure |
| 41873228190 | 13344694259 | JVM | 60 | 60 | 0 | 0 | 0 | failure |
| 41874166590 | 13345348690 | Android | 24 | 24 | 0 | 0 | 0 | success |
| 41874166590 | 13345348690 | JVM | 60 | 60 | 0 | 0 | 0 | success |
| 41878171031 | 13347802808 | Android | 39 | 38 | 4 | 0 | 1 | failure |
| 41878171031 | 13347802808 | JVM | 73 | 73 | 0 | 0 | 0 | failure |
| 41880252503 | 13348414922 | JVM | 73 | 73 | 0 | 0 | 0 | cancelled |
| 41883615254 | 13350217915 | Android | 39 | 38 | 3 | 0 | 1 | failure |
| 41883615254 | 13350217915 | JVM | 73 | 73 | 0 | 0 | 0 | failure |
| 41884537905 | 13349999077 | JVM | 73 | 73 | 0 | 0 | 0 | cancelled |
| 41884722256 | 13351591434 | Android | 39 | 38 | 1 | 0 | 1 | failure |
| 41884722256 | 13351591434 | JVM | 73 | 73 | 0 | 0 | 0 | failure |
| 41886672130 | 13353005045 | Android | 41 | 40 | 0 | 0 | 1 | success |
| 41886672130 | 13353005045 | JVM | 73 | 73 | 0 | 0 | 0 | success |
| 41890292122 | 13354960248 | Android | 64 | 63 | 1 | 0 | 1 | failure |
| 41890292122 | 13354960248 | JVM | 74 | 74 | 0 | 0 | 0 | failure |
| 41891475431 | 13353517717 | Android | 64 | 63 | 0 | 0 | 1 | success |
| 41891475431 | 13353517717 | JVM | 74 | 74 | 0 | 0 | 0 | success |
| 41896259632 | 13356596683 | Android | 71 | 70 | 0 | 0 | 1 | success |
| 41896259632 | 13356596683 | JVM | 82 | 82 | 0 | 0 | 0 | success |
| 41928763689 | 13369718793 | Android | 94 | 93 | 0 | 0 | 1 | failure |
| 41928763689 | 13369718793 | JVM | 82 | 82 | 0 | 0 | 0 | failure |
| 41929851364 | 13370489833 | Android | 94 | 93 | 0 | 0 | 1 | success |
| 41929851364 | 13370489833 | JVM | 82 | 82 | 0 | 0 | 0 | success |
| 41932103128 | 13372128554 | Android | 104 | 103 | 2 | 0 | 1 | failure |
| 41932103128 | 13372128554 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41933012158 | 13371489682 | Android | 105 | 104 | 0 | 0 | 1 | success |
| 41933012158 | 13371489682 | JVM | 88 | 88 | 0 | 0 | 0 | success |
| 41935322993 | 13374111460 | Android | 110 | 109 | 1 | 0 | 1 | failure |
| 41935322993 | 13374111460 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41936236159 | 13375050677 | Android | 110 | 109 | 0 | 0 | 1 | success |
| 41936236159 | 13375050677 | JVM | 88 | 88 | 0 | 0 | 0 | success |
| 41937243378 | 13374664030 | Android | 377 | 376 | 1 | 0 | 1 | failure |
| 41937243378 | 13374664030 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41938835493 | 13374943779 | Android | 378 | 377 | 0 | 0 | 1 | success |
| 41938835493 | 13374943779 | JVM | 88 | 88 | 0 | 0 | 0 | success |
| 41943762519 | 13377528578 | Android | 378 | 377 | 0 | 0 | 1 | success |
| 41943762519 | 13377528578 | JVM | 88 | 88 | 0 | 0 | 0 | success |
| 41947162401 | 13379308335 | Android | 378 | 377 | 1 | 0 | 1 | failure |
| 41947162401 | 13379308335 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41948538632 | 13380137146 | Android | 378 | 377 | 0 | 0 | 1 | failure |
| 41948538632 | 13380137146 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41968727319 | 13389102578 | Android | 378 | 377 | 0 | 0 | 1 | failure |
| 41968727319 | 13389102578 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41970434767 | 13390005982 | Android | 378 | 377 | 0 | 0 | 1 | failure |
| 41970434767 | 13390005982 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41972826140 | 13391120749 | Android | 379 | 378 | 0 | 0 | 1 | failure |
| 41972826140 | 13391120749 | JVM | 88 | 88 | 0 | 0 | 0 | failure |
| 41974364095 | 13391716581 | Android | 379 | 378 | 0 | 0 | 1 | success |
| 41974364095 | 13391716581 | JVM | 88 | 88 | 0 | 0 | 0 | success |

## Machine-readable source inventories

- [Git files and blob/SHA-256 comparisons](../../evidence/git-inventory.json)
- [All historical runs](../../evidence/github-runs.json)
- [All Artifact metadata including expiry/digests](../../evidence/github-artifacts.json)
- [All Releases/assets](../../evidence/github-releases.json)
- [Protected ref snapshot](../../evidence/protected-refs.txt)
- [Recovery totals](../../evidence/recovery-summary.json)
- [XML counts](../../evidence/xml-counts.json)

## Codex-only material

Relevant `/tmp/gardenia-p0-*` stage report directories were inventoried against the fresh GitHub Artifact downloads. Original raw XML/PNG/JSON evidence has now been recovered from its authoritative Artifact rather than assigning unverified local provenance. Temporary watch logs, duplicate extracted reports, APKs, analysis scripts and local compiler caches stay outside Git. Existing technical findings are already incorporated in P0_FINAL_TECHNICAL_REVIEW.md and p0-final-review.json. No uncommitted historical report was found in the repository working tree.

## Limitations

Some early historical failed runs never uploaded a report Artifact. Their run metadata/log links are retained; no XML result is invented. Physical H303/RF/GNSS/lock-screen/power evidence does not exist in CI and remains NEEDS_DEVICE_VALIDATION. Artifact expiration may prevent future recovery of the explicitly non-selected bulky logs; the selected key raw evidence is now durable in Git.

## Recovered Codex-only historical documents

[Provenance and exact hashes](../../evidence/codex-local/provenance.json). Ten original report/PR/release-note/planning snapshots were found outside the repository, classified D and recovered unchanged. In particular the rc1.5 final delivery report had not previously been in Git. Planning snapshots are explicitly historical/unverified working notes; their pending states and superseded instructions do not govern P0.2. Their old relative links retain original context, and are not the current report index.

## Public APK verification

[Original public Release assets and independent verification](../../evidence/releases/). All four public APKs were freshly downloaded, sizes/GitHub digests matched, package identity checked using aapt2, and actual APK signatures verified against the fixed certificate using official Android apksig. APK bytes remain outside Git. Small original build-info/checksum/delivery assets are retained.

## Archive acceptance

PASSED: archive commit `eed6c6e239aa9be3e831c83f1bcd6b782fa68f8f`, [cloud validation 42179918190](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42179918190), and [independent remote verification receipt](../feature-p0-2-gps-ui/archive-verification.json). 1,821 tracked report/screenshot/checksum paths matched remote Git blob hashes; newly recovered E/C/D files are now A. Protected original refs are unchanged.

# Runner Gardenia reports

## Latest delivery: P0.2

[Final delivery report](branches/feature-p0-2-gps-ui/05-delivery-report.md) · [real-device checklist](branches/feature-p0-2-gps-ui/06-device-checklist.md) · [test Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.2) · [direct signed APK](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.2/RunnerGardenia-P0-Release.apk) · [remote seal](branches/feature-p0-2-gps-ui/final-verification.json).

P0.2/6773, com.fox.runner, fixed original certificate,2753061bytes, SHA25648c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046. Source4454d71; [publication recovery](branches/feature-p0-2-gps-ui/stage6-publication-recovery.md) succeeded without rebuilding/re-signing. Android395/394 executed/0 failures/0 errors/1 original skip; JVM110; original-build helpers66; recovery helpers71. Physical H303/RF/outdoor GNSS/OEM background/power remain NEEDS_DEVICE_VALIDATION. BLE reconnect was audited, not fixed.

## P0 and P0.2

- [Historical P0 index](branches/feature-p0-core-migration/README.md)
- [Complete historical inventory](branches/feature-p0-core-migration/INVENTORY.md)
- [P0 delivery](../P0_DELIVERY_REPORT.md), [device acceptance checklist](../P0_DEVICE_CHECKLIST.md)
- [P0.2 baseline and progress](branches/feature-p0-2-gps-ui/00-baseline.md)
- [P0.2 read-only BLE reconnect audit](branches/feature-p0-2-gps-ui/01-ble-reconnect-audit.md)
- [P0.2 GPS preparation: implementation and acceptance/failure evidence](branches/feature-p0-2-gps-ui/02-gps-state-acceptance.md)
- [P0.2 home UI acceptance and screenshot matrix](branches/feature-p0-2-gps-ui/03-ui-acceptance.md)

- [P0.2 full regression](branches/feature-p0-2-gps-ui/04-regression-report.md)
- [P0.2 signing/publication recovery and exact evidence](branches/feature-p0-2-gps-ui/stage6-publication-recovery.md)
- [P0.2 installed-Release screenshots and manifest/signature proofs](branches/feature-p0-2-gps-ui/evidence/stage6-run-42251766485/signed/)

## Heart-rate development

- [Architecture audit](../HEART_RATE_IMPLEMENTATION_AUDIT.md)
- [Stage acceptance, including rejected/failed runs](../HEART_RATE_STAGE_ACCEPTANCE.md)
- [UI review](../HEART_RATE_UI_REVIEW.md), [original screenshots and checksums](../screenshots/heart-rate/)
- [Real BLE device checklist](../HEART_RATE_BLE_TEST_CHECKLIST.md)
- [Historical tag audit](../RELEASE_TAG_AUDIT.md)

## Published version evidence

| Version | Release | Source | Actual CI and report Artifact |
|---|---|---|---|
| rc1.4 / 6769 | [Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.4) | `24aa0d7d36d244852e796cca6307936e34d989ed` | [41685585228](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41685585228), [13253724407](evidence/artifacts/13253724407/manifest.json) |
| rc1.5 / 6770 | [Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.5) | `270cfa49a1c94d658d16dfc1e84563765aee832f` | [41738430305](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41738430305), [13279853488](evidence/artifacts/13279853488/manifest.json) |
| rc1.6 / 6771 | [Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.6) | `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169` | [41848524070](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41848524070), [13332512386](evidence/artifacts/13332512386/manifest.json) |
| P0.1 / 6772 | [Test Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.1) | `3116e5fa7a56ce6f299ea45d5a45745f1aed4466` | [41974364095](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095), [APK 13391972069](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391972069), [reports 13391716581](evidence/artifacts/13391716581/manifest.json) |
| P0.2 /6773 | [Test Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.2) | `4454d716a5ae010b92d3eefa1ac1c0ebedde0063` | [checks SUCCESS](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485/job/128008421881), [signed upgrade SUCCESS](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485/job/128015231707), [recovered publication SUCCESS](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42255705842/attempts/4), [APK13522176163](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485/artifacts/13522176163), [committed exact proof](branches/feature-p0-2-gps-ui/stage6-verification.json) |

Raw XML, screenshots and JSON are exact-byte content-addressed objects. [Each Artifact manifest](evidence/artifacts/) maps original member paths to their retained file. [XML totals](evidence/xml-counts.json), [Git inventory](evidence/git-inventory.json), [GitHub metadata](evidence/github-releases.json) and [recovery summary](evidence/recovery-summary.json) make provenance auditable. Historical failed/cancelled runs are retained as failures, never promoted to passed acceptance. Upstream OpenTracks docs and application SVG resources remain separate from Runner Gardenia acceptance evidence.

- [Recovered rc1.5 final delivery report](evidence/codex-local/gardenia-final-delivery.md)
- [Other Codex-only historical documents and provenance](evidence/codex-local/provenance.json)
- [Verified public Release assets](evidence/releases/)

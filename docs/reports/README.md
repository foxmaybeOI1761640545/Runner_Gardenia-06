# Runner Gardenia reports

## P0 and P0.2

- [Historical P0 index](branches/feature-p0-core-migration/README.md)
- [Complete historical inventory](branches/feature-p0-core-migration/INVENTORY.md)
- [P0 delivery](../P0_DELIVERY_REPORT.md), [device acceptance checklist](../P0_DEVICE_CHECKLIST.md)
- [P0.2 baseline and progress](branches/feature-p0-2-gps-ui/00-baseline.md)
- [P0.2 read-only BLE reconnect audit](branches/feature-p0-2-gps-ui/01-ble-reconnect-audit.md)
- [P0.2 GPS preparation: implementation and current acceptance/failure evidence](branches/feature-p0-2-gps-ui/02-gps-state-acceptance.md)

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

Raw XML, screenshots and JSON are exact-byte content-addressed objects. [Each Artifact manifest](evidence/artifacts/) maps original member paths to their retained file. [XML totals](evidence/xml-counts.json), [Git inventory](evidence/git-inventory.json), [GitHub metadata](evidence/github-releases.json) and [recovery summary](evidence/recovery-summary.json) make provenance auditable. Historical failed/cancelled runs are retained as failures, never promoted to passed acceptance. Upstream OpenTracks docs and application SVG resources remain separate from Runner Gardenia acceptance evidence.

- [Recovered rc1.5 final delivery report](evidence/codex-local/gardenia-final-delivery.md)
- [Other Codex-only historical documents and provenance](evidence/codex-local/provenance.json)
- [Verified public Release assets](evidence/releases/)

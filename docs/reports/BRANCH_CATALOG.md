# Runner Gardenia branch catalog

Remote snapshot: **2026-11-07T09:54:56.573566Z**. [Machine-readable capture](backup/BRANCH_SNAPSHOT.json). These are actual remote heads at capture, before committing this catalog; a document cannot contain its own future commit SHA. Use `git ls-remote --heads origin` for live heads. Later report-only commits do not change accepted APK source commits.

## Five existing branches

| Branch | HEAD at capture | First difference commit / UTC time |
|---|---|---|
| `design/p0-ui-lab` | `335da1bbccb5216715e99d15175f34e5aa1dbfec` | `495cb12026584878cbda4239aee303890ba8797b` / 2026-11-06T17:49:36Z |
| `feature/heart-rate-monitoring` | `121ea381fd11111427073fe291dfb5ddabf8b03e` | `a59a011510c1eaf6e6e2a6b276fdb8e72280706a` / 2026-11-03T18:12:37Z |
| `feature/p0-2-gps-ui` | `c787de60025a5cf435131babc062c1c17602e9db` | `eed6c6e239aa9be3e831c83f1bcd6b782fa68f8f` / 2026-11-06T07:17:29Z |
| `feature/p0-core-migration` | `60daeb21aa50bdbceec6e6ad354a431b8c1be434` | `26ae49620485962935946c036d716f057241f826` / 2026-11-04T16:34:44Z |
| `main` | `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169` | `f4efe3e0d37684ead48e4b127ea823ff19f1c96a` / 2026-11-03T12:20:19Z |

First difference times are **commit times**, an inference bound for development activity, not exact branch creation times. `main` has no parent branch: its row is the first reachable root snapshot. Heart-rate base24aa is documented by its architecture audit and PR#1 base; P0/P0.2 bases come from their frozen records; UI Lab explicitly derives from P0.1 source3116, not final P0/P0.2.

## Responsibilities and permanent entry points

### main

Android official line; stable baseline, no product changes authorized by this task. Includes merged heart-rate history.

Source: independent imported root snapshot; no original upstream Git history imported.

- [README.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/9b5d7b06afc1eb8e3315ba73874ac088fa4d5169/README.md)
- [BUILDING_RUNNER_GARDENIA.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/9b5d7b06afc1eb8e3315ba73874ac088fa4d5169/BUILDING_RUNNER_GARDENIA.md)
- [docs/HEART_RATE_STAGE_ACCEPTANCE.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/9b5d7b06afc1eb8e3315ba73874ac088fa4d5169/docs/HEART_RATE_STAGE_ACCEPTANCE.md)

### feature/heart-rate-monitoring

Completed heart-rate development/history; merged through PR#1 into main. Keep original reports and failures; no ongoing feature work in this task.

Documented base: `24aa0d7d36d244852e796cca6307936e34d989ed`.

- [docs/HEART_RATE_IMPLEMENTATION_AUDIT.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/121ea381fd11111427073fe291dfb5ddabf8b03e/docs/HEART_RATE_IMPLEMENTATION_AUDIT.md)
- [docs/HEART_RATE_UI_REVIEW.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/121ea381fd11111427073fe291dfb5ddabf8b03e/docs/HEART_RATE_UI_REVIEW.md)
- [docs/HEART_RATE_BLE_TEST_CHECKLIST.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/121ea381fd11111427073fe291dfb5ddabf8b03e/docs/HEART_RATE_BLE_TEST_CHECKLIST.md)

[PR#1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/1) merged at270cfa49a1c94d658d16dfc1e84563765aee832f. Its release is a main merge result, not a Tag on the feature HEAD.

### feature/p0-core-migration

P0 core migration complete; retained historical branch, not merged to main. Actual H303/GNSS/OEM checks remain NEEDS_DEVICE_VALIDATION.

Documented base: `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169`.

- [BUILDING_P0.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/60daeb21aa50bdbceec6e6ad354a431b8c1be434/BUILDING_P0.md)
- [docs/P0_DELIVERY_REPORT.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/60daeb21aa50bdbceec6e6ad354a431b8c1be434/docs/P0_DELIVERY_REPORT.md)
- [docs/P0_FINAL_TECHNICAL_REVIEW.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/60daeb21aa50bdbceec6e6ad354a431b8c1be434/docs/P0_FINAL_TECHNICAL_REVIEW.md)

### feature/p0-2-gps-ui

Native P0.2 GPS preparation/home UI complete; permanent historical archive and current backup maintenance. BLE reconnect was audited, not repaired; hardware acceptance pending.

Documented base: `60daeb21aa50bdbceec6e6ad354a431b8c1be434`.

- [docs/reports/README.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/c787de60025a5cf435131babc062c1c17602e9db/docs/reports/README.md)
- [docs/reports/branches/feature-p0-2-gps-ui/05-delivery-report.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/c787de60025a5cf435131babc062c1c17602e9db/docs/reports/branches/feature-p0-2-gps-ui/05-delivery-report.md)
- [docs/reports/branches/feature-p0-2-gps-ui/06-device-checklist.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/c787de60025a5cf435131babc062c1c17602e9db/docs/reports/branches/feature-p0-2-gps-ui/06-device-checklist.md)

Owns the complete66-Artifact historical manifests and Codex-local provenance. See [backup inventory](backup/BACKUP_INVENTORY.json); do not duplicate all2,056 original archive files or UI Lab assets here.

### design/p0-ui-lab

Independent long-lived HTML/CSS/JS design laboratory; not merged and not the Android app. Simulations are not device acceptance. Current task only archives evidence/documents.

Documented base: `3116e5fa7a56ce6f299ea45d5a45745f1aed4466`.

- [ui-lab/README.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/335da1bbccb5216715e99d15175f34e5aa1dbfec/ui-lab/README.md)
- [ui-lab/docs/acceptance/2026-11-07-run-42354349622.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/335da1bbccb5216715e99d15175f34e5aa1dbfec/ui-lab/docs/acceptance/2026-11-07-run-42354349622.md)
- [ui-lab/docs/NATIVE_UI_IMPLEMENTATION_GUIDE.md](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/335da1bbccb5216715e99d15175f34e5aa1dbfec/ui-lab/docs/NATIVE_UI_IMPLEMENTATION_GUIDE.md)

## Actual Git relationships

| Comparison | Status | Ahead / behind | Merge base |
|---|---|---|---|
| main → feature/heart-rate-monitoring | behind | 0 / 2 | `121ea381fd11111427073fe291dfb5ddabf8b03e` |
| main → feature/p0-core-migration | ahead | 48 / 0 | `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169` |
| feature/p0-core-migration → feature/p0-2-gps-ui | ahead | 30 / 0 | `60daeb21aa50bdbceec6e6ad354a431b8c1be434` |
| feature/p0-core-migration → design/p0-ui-lab | diverged | 21 / 5 | `3116e5fa7a56ce6f299ea45d5a45745f1aed4466` |
| feature/p0-2-gps-ui → design/p0-ui-lab | diverged | 21 / 35 | `3116e5fa7a56ce6f299ea45d5a45745f1aed4466` |

UI Lab and final P0/P0.2 are **diverged**, not a chain to be merged automatically. P0.2-only archive files missing from design/main are a responsibility boundary, not permission to copy them. `AGENTS.md` and several P0 documents differ by historical context; never overwrite them with a cross-branch synchronization.

## Existing deliveries (no Tags created by this task)

| Tag | Accepted source | Location |
|---|---|---|
| v0.1.0-rc1.4 | `24aa0d7d36d244852e796cca6307936e34d989ed` | [existing Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.4) |
| v0.1.0-rc1.5 | `270cfa49a1c94d658d16dfc1e84563765aee832f` | [existing Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.5) |
| v0.1.0-rc1.6 | `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169` | [existing Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.6) |
| test-p0-v0.1.0-p0.1 | `3116e5fa7a56ce6f299ea45d5a45745f1aed4466` | [existing Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.1) |
| test-p0-v0.1.0-p0.2 | `4454d716a5ae010b92d3eefa1ac1c0ebedde0063` | [existing Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.2) |

UI Lab has no independent APK Tag/Release. It references P0.1 and publishes a design website through existing Pages. Reports do not move Tags, regenerate APKs or imply physical-device verification.

## Maintenance

Update the snapshot and this directory when branch responsibilities change; preserve older snapshots by Git history. Follow [governance](BRANCH_GOVERNANCE.md), [workspace closeout](backup/WORKSPACE_SYNC_CHECKLIST.md), and [restore/verify](backup/RESTORE_AND_VERIFY.md). This directory is a lightweight cross-branch index, not a central copied archive or a new `project-records` branch.

# Clean navigation baseline and Stage 1 candidate

The user task supersedes earlier AGENTS branch/stage restrictions. Work starts from accepted documentation HEAD ae68319361c1277e97e4fe9bfc9ae52de88146e5 on feature/p0-7-to-p1-0-offline-navigation-clean. The accepted APK/test source is 6cee1958a23bfd1a1fe3b6a9b547717c528cbc75. The diff between them contains only documentation and acceptance receipts.

All remote heads and tags were fetched again and captured in protected-refs.txt. The accepted branch and reference-only experiment match the user-supplied SHAs. No main/experiment mutation, merge or cherry-pick was performed. The experiment's failed API35 run 43696204580 remains failed. No experimental production source was copied into this implementation.

The version audit contains all 11 Release build-info files and all remote branch version properties/ledgers. Highest published/reserved effective code is 6779. The experiment's 6780 is an unpublished version property without a corresponding reservation or release. 6780 is available as the first P0.7 candidate, subject to another remote audit when actually reserving it. Version properties and ledger remain unchanged during Stage 1.

## Architecture and stage boundaries

1. Stage 1 retains all four XML home binding contracts and replaces the illustrative RouteMapView internals with OfflineMapView. Private file selection uses OfflineMapRepository; all file reads run on a serial worker. Cache capacity remains <=64 and <=32MiB, frame overdraw remains 1.0. onStop/detach closes renderer/file/cache before another map page can own a renderer. Position/route come from the existing recording ViewModel; no GPS listener/owner is added in this stage. Accuracy uses a geodesic-meter Mapsforge Circle. Gesture drag suspends follow; explicit recenter resumes it. Camera/follow state survive activity recreation. Empty-file state still provides a neutral camera/position overlay. Coverage probes inspect actual feature data at the camera's tile/zoom, never infer it from the bounding box. This is a tile-feature statement, not a claim of administrative or complete road coverage.
2. Only after Stage 1 passes: introduce a single application-foreground owner using Startup lifecycle callbacks and the existing service ownership path. Internal activity transitions retain one owner; background idle releases it. Reuse the existing latest GPS state and monotonic freshness checks.
3. Only after Stage 2 passes: independently verify/repair preview-to-recording handover, preserving user GPS settings, existing recording boundaries and all original ownership assertions. No BLE/SQLite/export redesign.
4. Stages 4–7 remain blocked until their explicitly preceding acceptance/publication gates pass. No route library, routing or gait production code is introduced here.

## Current evidence

Local Python helpers: Tests 141, Executed 141, Failures 0, Errors 0, Skipped 0. Android resource XML parses and git diff whitespace check passes. No Android SDK or local APK build is used. JVM/API35/lint/native screenshots are pending the exact-source new Actions run; none is claimed passed. New P07HomeMapTest exercises no-map, actual 105,834,033-byte map, feature-empty tile within bbox, outside-map, follow/pan/zoom, recreation, map replacement/deletion, budget and actual themed screenshots. The complete original Android and JVM suites run unchanged.

Stage 1 is a candidate, NOT accepted or published. No signing job exists in this initial validation-only workflow; signing/publication will be added only with explicit fail-closed dependencies and accepted visual evidence before P0.7 delivery. No APK, tag or release is created by this commit.

Hardware GPS/battery, Xiaomi CIVI 5 Pro, Redmi Note 12 Pro+ and Magene H303 remain NEEDS_DEVICE_VALIDATION. Emulator evidence cannot close those items.

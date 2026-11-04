# P0 offline runtime audit

Scope: feature/p0-core-migration only; application ID com.fox.runner remains stable.
Automatic acceptance is recorded in P0_ACCEPTANCE.md and p0-evidence/stage-8.json.
This report describes the implemented policy; actual merged/APK/native evidence
must pass before stage 9. A source review alone is not an APK acceptance result.

| Area | P0 policy |
| --- | --- |
| Network | INTERNET is explicitly removed at manifest merge. No account, HTTP client, cloud API, analytics, advertising, upload queue or online map is introduced. |
| Location | Existing local Android GPS provider, one-second request and mature recording/statistics; no fused/cloud location provider. GPS starts for a workout, not idle HR preview. |
| BLE | Existing local standard SIG GATT driver. Only the selected HR device is connected; remembered non-P0 cadence/power/temperature/barometer devices are not started. |
| Storage | Existing database.db, schema 43 and repositories. No second production DB, network storage or cloud sync. |
| System backup | allowBackup=false; explicit exclusions cover root/file/database/sharedpref/external and device-protected equivalents for legacy backup, cloud backup and device transfer. |
| Export | GPX to private cache and an explicit user-chosen Android document/share action. System destination apps are the user's choice; P0 does not invoke a remote API. |
| Speech | P0 core service does not initialize or call an external TTS engine. |
| Release components | Only existing launcher compatibility route, P0 home/sensors/history, private local data/FileProviders, recording service and disabled app-locale metadata service. No health integration, widgets, public recording API, legacy import/export/settings entry points or WorkManager startup/boot receivers. |
| Permissions | Release allows only GPS, BLE, foreground recording, wake/vibration, optional notifications and the AndroidX private receiver signature permission. Health/network-state/boot permissions are removed. |
| Crash handling | Release uses Android platform handling; the legacy browser-based error-report UI is not a production entry point. Debug keeps original regression components. |

Existing dependency versions and legacy source/test fixtures remain so that the
original regression suite can still compile and execute. They do not create a
second runtime architecture. Release manifest pruning removes their component
roots; stage 10 enables and inspects R8/resource shrinking. No dependency version
is upgraded, and unused legacy files are not deleted to make an APK look smaller.

Source URL matches were license/documentation references, XML namespaces, legacy
migration strings or explicit external intents. A namespace URL is not a network
request. The P0 user flow contains no online map, browser report, login or upload.
Old DB migrations and stored historical URLs are retained for data compatibility.

CI requires native package/backup-domain/sensor-dispatch/permission/recovery tests,
an actual packaged Debug manifest audit and a Gradle-produced Release merged
manifest audit without any release signing secrets. Host audit negative tests
reject INTERNET in either build, unexpected Release permissions/components,
exported private components and enabled backup. The actual final signed Release
APK receives the same runtime policy checks in stage 10.

Physical airplane-mode cold GPS acquisition, H303 RF/reconnect, background/OEM
restrictions, reboot and battery use remain NEEDS_DEVICE_VALIDATION. Emulator and
source evidence cannot establish those hardware properties. Optional notification
denial does not gate a GPS recording; preview uses only its connected-device
foreground capability. Missing system recovery records stop non-sticky safely.

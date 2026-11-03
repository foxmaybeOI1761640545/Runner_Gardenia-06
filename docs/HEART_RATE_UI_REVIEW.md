# Heart-rate UI acceptance

Actual API 35 managed-device run: [41730815364](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41730815364), feature commit db08d13b920e4b668b556983efe5db205c10d000.

40 full-screen PNGs were produced by UiAutomation, with actual light/dark resource configuration and 411dp (1080×2400) / 360dp (720×1280) windows. The complete set is in the run's check-reports Artifact. Twelve unedited key captures are retained here; SHA256SUMS identifies their original bytes, and capture-manifest.json describes the complete 40-image source set.

The recording, stale, chart, settings/scan, original home and historical-record pages were inspected. The final narrow-screen captures show the intended BPM/state without mixed Activity transitions. Text wraps, the native settings/history lists scroll, recording controls remain accessible, and the Material palette/navigation remain consistent. The default recording grid is still user-configurable; this work does not fill unused space with invented data.

| Page | Evidence |
| --- | --- |
| Live recording, normal light | [PNG](screenshots/heart-rate/phone-light-recording-connected.png) |
| Stale measurement, normal dark | [PNG](screenshots/heart-rate/phone-dark-recording-stale.png) |
| Live recording, narrow light/dark | [Light](screenshots/heart-rate/narrow-light-recording-connected.png), [dark](screenshots/heart-rate/narrow-dark-recording-connected.png) |
| Time chart and extrema | [Normal light](screenshots/heart-rate/phone-light-time-chart.png), [narrow dark](screenshots/heart-rate/narrow-dark-time-chart.png) |
| Existing distance chart | [PNG](screenshots/heart-rate/phone-dark-distance-chart.png) |
| Sensor settings/preview/controls | [Normal dark](screenshots/heart-rate/phone-dark-settings.png), [narrow dark](screenshots/heart-rate/narrow-dark-settings.png) |
| Device list/RSSI | [Narrow light](screenshots/heart-rate/narrow-light-scan-fixture.png) |
| Original home/history | [Home](screenshots/heart-rate/narrow-dark-home.png), [history](screenshots/heart-rate/narrow-light-history.png) |

BPM/device rows are explicit test fixtures, not physical heart-strap observations. The real settings/dialog and recording/history screens are exercised; the emulator radio is deliberately offline because its virtual Bluetooth stack aborted on a real scan request. Radio compatibility, RF reconnection and battery endurance need the hardware checklist. Animation scales, heads-up setting, theme, density, size and original Bluetooth power are restored after the screenshot test. Production code never generates fixture BPM or changes the user's timezone.

CI requires every expected PNG, valid dimensions/profile metadata, all instrumentation XML, nonzero actual test execution and zero failures/errors. It does not replace manual image inspection. A previous green run's transitional captures were rejected and recollected before stage 3 acceptance.

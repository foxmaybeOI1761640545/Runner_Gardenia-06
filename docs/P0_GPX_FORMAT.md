# P0 local GPX format

P0 exports GPX 1.1 through Android CreateDocument or an explicit read-only system
share action. Export runs on a worker. Finish an active/paused workout first.
There is no account, network request or automatic upload. A system document
provider chosen by the user owns its destination; failed writes are reported,
not silently accepted or automatically deleted. Private incomplete cache files
are removed. Completed share files stay in the private p0_exports cache and are
eligible for cleanup after seven days.

## Coordinates and time

Only stored coordinate-bearing TrackPoints become trkpt. No location is invented
for a heart-rate-only sample or workout. Manual pause/resume, automatic GPS gaps
and idle boundaries form distinct trkseg elements. Existing SQLite microdegree
coordinate precision is retained. The equivalent antimeridian +180 longitude is
written as -180 because GPX longitude has an exclusive upper limit of +180.
Numbers use plain decimal notation independently of the device locale. Times
are UTC ISO-8601 instants, preserving their absolute time; raw sample timestamps
retain their stored precision. Optional elevation, accuracy and speed are kept.

## Heart-rate matching

For standard Garmin TrackPointExtension/v2 hr, choose the closest notification
within 1000ms and within the segment's first/last GPS timestamp, inclusive. Equal
distance chooses the earlier GPS point. If several samples map to one point,
the closest sample is projected; equal distance keeps the earlier sample in
chronological database order. Single-point segments match only their exact time.
Samples in gaps/outside these ranges stay unmatched. Overlapping/backwards
imported segment time ranges disable this optional projection rather than guess.
BPM above the Garmin unsigned-byte range remains in the full raw extension.
Standard speed is meters per second. Existing point-only heart rate is used for
standard projection only when the workout has no raw notifications.

## Lossless application extension

Namespace: urn:runner-gardenia:gpx:1 (prefix rg). The root GPX extensions contains
rg:workout, identified by the original local track ID and UUID. It contains every
independent heart_rate_samples row in chronological order as rg:sample:

- id, time, bpm, source; deviceAddress/deviceName and connectionId when known.
- contact and energyExpended only when supplied.
- Each original RR interval is an rg:rr1024 child, in units of 1/1024 second.
- nearestPointId identifies an eligible real point, if one exists; it does not
  claim that every mapped sample is the one projected as the standard point HR.

Repeated BPM notifications retain different sample IDs. Unmatched/no-GPS samples
are never dropped or converted into invented coordinates. Point-only legacy
heart rates, including sensor points without coordinates, remain separately as
rg:legacyHeartRate with pointId/time/bpm; no BLE notification IDs are fabricated.
The file includes the requested device metadata and is shared only by user action.
Other GPX applications can read the standard track while ignoring this extension;
round-tripping through applications that discard unknown extensions cannot
preserve the complete raw sample stream.

Host CI validates the actual Android-produced files against the pinned official
GPX 1.1 XSD without remote schema/DTD resolution. Native tests additionally check
raw metadata, timestamps, counts, gaps, no-GPS/no-HR and local save/share contracts.

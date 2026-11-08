# Accepted APK publication: Actions log media type

Source `77ef053b76fd5e7c6c0c9ce1023e365fa3d7c471`, run [42443873771](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42443873771), publication job `128623344922`.

The full checks, independent API35 window helper, fixed-key signed Release, actual P0.2 upgrade, GPS/GPX checks and eight-scheme signed APK matrix all passed. Publication failed before creating a Tag or Release while requesting the successful checks job log with the Release-asset `application/octet-stream` media type. GitHub returned HTTP 415.

```text
2026-11-08T04:07:15.2635802Z Fixed-key P0.3 APK, actual same-run tests and P0.2 upgrade verified.
File "p03_publish.py", line 55, in publish
    helpers=helper_counts(api,info['plan']['runId'])
File "p03_publish.py", line 45, in helper_counts
    log=api.request(.../actions/jobs/.../logs,binary=True).decode('utf-8')
RuntimeError: GitHub GET failed with HTTP 415
2026-11-08T04:07:15.3284024Z Process completed with exit code 1.
```

Recovery uses the GitHub media type for Actions logs and artifact archives; only Release asset bodies use the octet-stream media type. A publication-only route rejects application, version, signing and original-test changes, verifies the original successful jobs and immutable ZIP checksums, and revalidates the exact already accepted APK. It does not rebuild or sign. The original failed run remains available.

Accepted APK SHA-256: `24225e5a28827eda1fa5799e80230d03464416a3aaa72c0cf34ccb4be090a3ff` (2,799,792 bytes). Fixed signer SHA-256: `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`.

# Public browser route gateway errors; anonymous public asset API verified

Run `42467882430`, source `065f1811e6c5517f3c53bc6a9b5dacec0104fdac`, attempt 1 uploaded and re-downloaded all eight Draft assets, verified the exact annotated Tag, then published the non-Latest Pre-release. The anonymous browser APK request returned HTTP 504, so no completed publication receipt was emitted. Attempt 2 reverified the existing assets and encountered the same public-browser transport failure. No asset or Tag was replaced.

Independent diagnostics also found HTTP 504 on the old P0.2 browser APK path. A query HEAD initially returned a GitHub asset redirect, but browser GET requests still returned gateway errors. The public GitHub asset API, using `Accept: application/octet-stream` and **no Authorization header**, returned a redirect to `release-assets.githubusercontent.com` and HTTP 200 APK bytes. The APK is exactly 2,799,792 bytes with SHA-256 `24225e5a28827eda1fa5799e80230d03464416a3aaa72c0cf34ccb4be090a3ff`, byte-identical to the original accepted build Artifact.

Use this anonymous public asset transport for final signature/identity verification, recording its URL, HTTP status, channel and absence of authentication explicitly. Keep the canonical browser download URL in the Release receipt for users. Do not describe the failing browser GET as verified. Extend the publication transport regression to require the public asset API media type and absence of Authorization.

Attempt 1 failure artifact: `P03-recovery-proof-42467882430-1`, ID `13611989261`, SHA-256 `a715e7e32a4a2f27addeb5bfa27d230e3a978af48e25c95212bf160c0497a642`. Original failures remain retained. No rebuild, re-signing or permission changes occurred.

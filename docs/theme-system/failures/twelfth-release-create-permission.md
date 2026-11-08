# Actions Release-creation permission failure, accepted APK retained

Publication-only run [42467539194](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42467539194), source `5b17ffa97a98e5cb959f3f51ffb3e220f33289ab`, attempt 1 passed all original ZIP/report/APK checks and the reviewed API35 recapture proof, then read the actual 68-test helper result successfully with the corrected media type. `POST /releases` returned HTTP 403 for the workflow integration token despite the job's declared `contents: write` permission.

The unique annotated Tag already points at accepted build source `77ef053b76fd5e7c6c0c9ce1023e365fa3d7c471`. No app rebuild, re-signing, asset replacement or modification of an existing Release occurred. Failure artifact `P03-recovery-proof-42467539194-1`, ID `13612366968`, SHA-256 `6b675189d3ecba6be8b100d5943295d698d9cec597b2f10f2ce2df3c2af9465d`, retains exact recovery inputs, test summary and notes.

The existing authorized repository connection created only the new P0.3 draft (`438149787`) with the exact tested source, notes, Pre-release and non-Latest settings. Retry the same failed workflow job to resume its unchanged idempotent asset verification and publication. No new Secret, signing key, repository permission setting or token was introduced.

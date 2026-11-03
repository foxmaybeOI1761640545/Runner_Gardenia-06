# Runner Gardenia agent instructions

## Repository and release identity

- Work only in `foxmaybeOI1761640545/Runner_Gardenia-06`, branch `main`, unless the user explicitly requests otherwise.
- Android application ID is `com.fox.runner`. Preserve the existing Java namespace unless a task explicitly requires changing it.
- Use `Asia/Singapore` as the CI and instrumentation timezone (UTC+08:00).
- The single source of release version metadata is `version.properties`.

## Stage completion and automatic release

- Do not create a Git tag or GitHub Release manually. The repository's GitHub Actions workflow creates the annotated version tag after the main-branch Debug CI passes, then dispatches the signed release run.
- Do not bump `version.properties` for intermediate commits. Update it only in the final, release-ready commit for a completed development stage. A main push with an untagged version and successful checks automatically promotes that version.
- The current first candidate is `v0.1.0-rc1.1` with Version Code `6766`. For the next candidate in the same RC series use `v0.1.0-rc1.2` and increment Version Code to `6767`; continue monotonically. New feature milestones may start a new version series, but Version Code must always exceed every previously published version.
- Never reuse an existing version tag, lower a Version Code, force-move a tag, or overwrite a published Release.

## Required completion procedure

When a user-requested development stage is complete and ready for a test release:

1. Finish implementation and run all relevant local/static checks available in the cloud environment. Do not claim Android build or device tests passed locally if they were not run.
2. Review the complete diff; preserve unrelated user changes and do not modify signing secrets or business logic outside the requested scope.
3. Update `version.properties` to the next release candidate name and strictly increasing Version Code in the final stage commit.
4. Commit and push the completed stage to `main`.
5. Wait for the GitHub Actions run for that exact main commit. Confirm the Debug build, Lint, unit-test task, API 35 instrumentation tests, report gate, APK identity verification, and Debug APK Artifact upload all succeeded.
6. The `create-tag` job should then create the exact version tag and dispatch the signed release run. Do not create the tag yourself.
7. Wait for the tag's release run. Confirm the repeated checks, fixed-key signing, certificate fingerprint, APK identity and release gates succeeded; confirm the GitHub Pre-release exists and contains the APK, `SHA256SUMS`, and `build-info.json`.
8. Report the exact commit, tag, Actions run, Pre-release URL, APK filename, and checksum. If any of these cannot be verified, state precisely what remains incomplete.

## Failure handling

- If main CI fails before the tag is created, fix the issue and push another commit with the same candidate version. Once CI passes, the automatic tag job can create it; do not increment Version Code just to retry an untagged candidate.
- If a tag already exists on an earlier commit with the same version metadata, the workflow skips it. Do not try to retarget it. Prepare a new version name and increment Version Code for a new release.
- If the signed release run fails after the tag was created, never force-move or reuse that tag. For code fixes, create a new candidate with a higher Version Code. For a transient infrastructure failure, rerun the existing tag workflow without changing the tag.
- A manual `workflow_dispatch` defaults to CI-only. The internal automatic release dispatch explicitly passes `release=true`; do not use that input as a shortcut around the main CI gate.
- Never claim an APK was published merely because a build or Artifact upload succeeded. Verify the actual GitHub Pre-release and its assets.

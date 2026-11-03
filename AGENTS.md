# Runner Gardenia agent instructions

- Work only in foxmaybeOI1761640545/Runner_Gardenia-06, main, unless explicitly directed otherwise.
- Preserve application ID com.fox.runner, Java namespace and unrelated business logic.
- CI, JVM, managed device and instrumentation use Asia/Singapore (UTC+08:00). Production follows the user's device timezone.
- Use the checked-in Gradle Wrapper, AGP and Java compatibility settings. Do not install an Android SDK in Codex Cloud.

## Automatic publication

A push to main runs one workflow: prepare → checks → create-tag → signed → publish. PR/manual CI does not publish. Never manually create/move/delete Tags or manually publish a Release to bypass this chain.

version.properties defines a version-series seed and minimum Code. The workflow allocates the next candidate and strictly increasing Code, stores release-plan.json, and renders identical per-run version.properties for Debug and signed builds. Do not manually bump the seed for intermediate commits; change the series only for an explicitly requested milestone.

Annotated Tags store the exact tested commit, allocated version/Code and run ID. Preserve all existing Tags. Retry failed jobs to resume a run; do not rewrite its Tag. A normal new main commit allocates a new version after any already-tagged attempt. Git history is never automatically rewritten or augmented by version-bump commits.

## Completion evidence

Before committing, run relevant available static/script tests, inspect the diff and preserve user work. Push normally, without force. Wait for the exact main commit's complete workflow. Verify all Debug checks, nonzero instrumented execution with separate failures/errors/skips, device/process timezone tests, APK identity, immutable Tag, fixed signing fingerprint, APK checksum, actual Pre-release and uploaded assets.

Do not claim Android tests ran locally when they did not. A successful Debug workflow, Tag creation or signed Artifact alone is not publication. Report source SHA, run, actual counts, Tag, APK SHA-256, Pre-release and download URLs, and any remaining blocker.

Only signed accesses android-release Secrets. Do not generate or disclose signing material, alter credentials, add PATs or suppress failing tests. Published assets are verified and retained, never silently replaced.

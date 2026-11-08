# Read-only helper initialization failure

Source 22a19d37c1bfb732314a545a33582550945d3b0f, run 42439201443, signed job 128607368305. All original/new checks again passed (404 Android executed, 113 JVM, 68 helper tests, eight schemes). Fixed-key signing passed; publication was skipped.

The new standalone shell helper compiled successfully but omitted Android's main Looper initialization. Android 15 AccessibilityInteractionClient initializes a Handler against Looper.getMainLooper(), causing the helper process's NullPointerException (original excerpt retained alongside this report). This is a test utility crash, not an app crash.

Add Looper.prepareMainLooper() before connecting UiAutomation, matching the platform command bootstrap. Add an independent API35 helper job: compile with the existing cloud SDK, run two separate read-only shell processes against the actual Launcher, require fresh XML, real native IDs and API35, and retain diagnostics. Signing now requires this job as well as all full regression gates. No app code, business assertion, signing identity or dependency changes.

Original diagnostic artifact 13600988927, SHA-256 e50ee7cbc34c26d5d051b454bd896967456b0e1c935c458d6c8657f94da57f0d.

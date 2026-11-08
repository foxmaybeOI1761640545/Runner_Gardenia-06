# Signed screenshot visual review rejected stale pages and animations

The signed APK bytes `24225e5a28827eda1fa5799e80230d03464416a3aaa72c0cf34ccb4be090a3ff` and original full Android/JVM/runtime gates remain unchanged. A contact sheet review of the original run `42443873771` exposed screenshots taken during native window animation. Several Light heart/history captures still displayed the preceding home. Pixel matching alone did not validate page identity.

Publication recovery run [42446247564](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42446247564) was explicitly cancelled before any Tag or Release was created. The original artifact remains available; these captures are not final delivery screenshots.

The capture harness now disables and restores Android's three animation scales, requires the expected focused native Activity and unique page View ID, rejects a mixed/preceding page tree, waits again, and retains the XML tree next to every PNG. Regression tests explicitly reject stale home and mixed transition trees. The same already signed APK is installed again on a fresh API35 device, including the retained P0.2 upgrade/GPS/GPX suite. No application rebuild or signing occurs. Publication waits for the replacement screenshots to pass actual visual review.

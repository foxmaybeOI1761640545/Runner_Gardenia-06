# API35 focused-window query scope

Recapture-only run [42466295122](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42466295122), source `cb4390752f254fe12861b6ca2411bff3325c027d`, failed the strict first-home focus gate. The actual fresh [home XML](cb43907-actual-home.xml) and native screenshot showed the correct home; system logs independently showed `P0Activity` displayed. The narrow `dumpsys window windows` section omitted the current focus field, so the new focus assertion correctly refused to proceed without evidence. No publication was attempted.

Use full `dumpsys window`, which includes display focus, and retain its raw last snapshot for diagnosis. Unique page View IDs, animation disabling and delayed re-sampling remain required. Re-run the same immutable signed APK on a fresh API35 device; do not rebuild or sign.

The failed original artifact is retained as `P03-recaptured-proof-42466295122-1`, ID `13612165867`, SHA-256 `e82c52b3ab61cacee55026be8b6e1aba6dc0c6088248971c9240fb74ead1b573`.

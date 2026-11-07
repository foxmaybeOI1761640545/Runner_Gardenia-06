# Phase 1 delivery — paired theme system

The ten original family layouts are retained. Every family has complete Light/Dark tokens, independent manual appearance controls, browser System Preview, and a synchronized two-palette comparison. [Design and token specification](THEME_SYSTEM_PHASE1.md). [Original browser screenshot gallery](../previews/appearances/index.html).

Local acceptance passed: 1,303 original regression assertions; 60 theme/appearance/viewport combinations; 330 original browser captures; no JavaScript errors, resource failures or horizontal overflow. Both explicit overrides and system preference changes were exercised. All body/status/button semantic palette pairs meet 4.5:1; generated resources match the canonical JSON.

Viewports: 1440×1000, 1366×768, 390×844. The full original metric, GPS, BLE, recording, history/detail, heart chart, GPX, keyboard/touch and offline behavior suite is retained. Its direct-file offline test passed on official Playwright Chrome for Testing 151; the appearance matrix passed on Chromium 155. The managed-Chromium file-URL failure is preserved separately and was not treated as passing.

Local evidence: [regression](acceptance/theme-system/local-results.json), [palette matrix](acceptance/theme-system/local-appearance-browser.json), [local gate](acceptance/theme-system/local-phase1-gate.json), [source file hashes](acceptance/theme-system/source-files.json). The local Git SHA identifies the pre-commit base; source-files.json records exact tested working source bytes. Cloud acceptance will bind the committed source SHA.

Online deployment and live acceptance are pending. Phase 2 remains blocked until the live gate passes. This report will be updated with the exact Actions run, deployed source SHA, online evidence, and the Phase 2 handoff.

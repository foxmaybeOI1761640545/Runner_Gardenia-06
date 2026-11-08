# Fresh AVD Launcher readiness failure — retained

Full run 42499729832 / 90c6e402159cd91c278349815e249693dedd281a, independent helper job 128789684390. Original helper artifact 13625460762 / SHA256 5cef1e7e7420583b7ba7c6701cad75bf51c21cd668efce86d074ae3049a20b6e.

Failure: launcher tree absent immediately after first fresh AVD boot. Log shows FallbackHome -> SDK setup -> QuickstepLauncher, displayed only 3.8 seconds after launch. The original helper waited 2 seconds. No signing took place.

Fix: bounded fresh-window polling (30 seconds), require both real Launcher nodes and focused Launcher window, retain every attempt XML and focus. Two distinct shell processes, resource IDs and actual no/yes OS-mode queries remain mandatory. No assertion skipped or synthetic tree substituted.

# Native comparison locale was not applied

Run 42480868961, source10c1c9d: compile/Lint/JVM passed; actual API35 fixture launch failed `expected zh but was en`, before any screenshot was accepted. Artifact13617995852 SHA256cb75dcee371dbd1556068742c7844dffd3f644282dec57362ba258b3a5a06f5d preserves original XML.

The instrumented process did not apply the requested per-app language before the preview Activity resource lookup. Fix the language only in the Debug preview host using Android createConfigurationContext, inheriting the actual system night mask, density, font and viewport. Keep the strict zh assertion and all192 screenshots. Production resources, language policy and all business data paths stay unchanged.

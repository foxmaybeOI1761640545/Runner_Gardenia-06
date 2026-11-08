# API35 matrix test annotation

Source `e890f8169b29b3950396052ef12e780767461498`, run [42480562555](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42480562555), compile job `128731368542`. Java/Android instrumentation compilation succeeded; Lint rejected `@RequiresApi(35)` on a test class with `UseSdkSuppress`.

Use AndroidX `@SdkSuppress(minSdkVersion=35)`, the proper instrumentation API selector, for this API35-only locale/viewport matrix. The actual CI emulator is API35: the test must execute all 192 cases there and cannot be skipped in the final complete gate. No Lint baseline or disabled check was introduced. Production minimum API remains26; no production functionality was changed by this annotation fix.

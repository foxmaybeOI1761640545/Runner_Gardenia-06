# Publication upload import scope

Run `42467539194`, source `5b17ffa97a98e5cb959f3f51ffb3e220f33289ab`, attempt 2 successfully verified the accepted APK and located the separately created P0.3 draft. Before uploading any asset, Python raised `UnboundLocalError: cannot access local variable 'urllib' where it is not associated with a value` at `urllib.parse.urlencode`. A later function-local `import urllib.request` made `urllib` local throughout the function.

Move `urllib.request` to module scope with the existing `urllib.parse`. Add a publication transport regression that creates a draft, uploads all eight fixture assets, downloads and checks each before publication, rechecks all after publication, and requires the public APK to match. This fixture only tests publication transport and does not substitute for actual APK, signature or Android evidence. All 74 helper tests pass.

Failure artifact: `P03-recovery-proof-42467539194-2`, ID `13612441963`, SHA-256 `f6c8c7ff8f65289fe02f6c55277e9bdce7b5fb18b128f683f69b7a0599e552f1`. No APK rebuild/re-signing, asset overwrite or Tag movement occurred. Continue the existing draft using the exact same accepted APK and reviewed native evidence.

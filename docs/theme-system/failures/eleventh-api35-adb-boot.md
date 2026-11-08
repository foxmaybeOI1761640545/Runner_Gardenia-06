# Transient API35 adb root connection failure

Recapture-only run `42466614968`, source `49ae83b855fe43a3b50d6086280ec684a3dbe5b4`, attempt 1 ended immediately after emulator boot at the original unmodified test helper's `adb root`: `adb: unable to connect for root: closed`. It did not reach app installation or theme capture. The failed job/artifact is retained: `P03-recaptured-proof-42466614968-1`, ID `13612460086`, SHA-256 `f33b621e8b40e11336e3fd9752eb8d40802416cbdf61ecef293fc2f0b6f73aec`.

Retry only this failed job, using the same commit, test code, immutable APK, checks and assertions. No APK build, signing, Tag or Release operation occurs in this recapture job.

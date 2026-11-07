# First cloud candidate — preserved failure

Source: `f8b1b274abfa1052a988d964feba90182e61b9e5`; Run: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42389820892 ; checks job 128454856449.

Java/resource compilation completed. Lint failed with four NewApi errors: android:windowLightNavigationBar requires API27 while minSdk is 26 (one occurrence per family). Android/JVM execution after this step did not run; downstream absent GPX/UI evidence correctly failed closed. No signing, tag or publication occurred.

Fix: remove the XML attribute and retain AbstractActivity's WindowCompat appearance handling, which already serves the compatible runtime path. No lint rule is suppressed. The next commit reruns the full cloud build/test gate.

# Full gate workflow parse failure — retained

Source f20d1e8c3eaff61850f7da1741ae181069c92838, run 42499656468. GitHub rejected job-level env runner.temp expressions (lines 224/346); zero jobs started, zero tests/signing/publication.

Fix: use RUNNER_TEMP inside the Python signing context helper; remove unsupported job-level expression. Original failed run remains public. Native preview run 42499118275 is independent and remains intact.

The GPX 1.1 XSD is the published TopoGrafix schema:
https://www.topografix.com/GPX/1/1/gpx.xsd

Retrieved 2026-11-04. Upstream bytes SHA-256:
`9e4d1988b862edbe556305b130f8f6f1b29864fefd0dc02d5dab04ccdd1f34d6`.
Only CRLF was normalized to LF; the normalized checksum is recorded alongside it.
This file is a host CI test fixture, never an Android runtime dependency.
Its original indentation/trailing whitespace is intentionally retained, with a
path-specific Git whitespace attribute for this third-party fixture only.
Validation forbids network schema/DTD resolution. Foreign namespace extensions
are permitted by the published GPX schema; native tests independently check raw
sample fields, exact times, counts, segment boundaries and unmatched samples.

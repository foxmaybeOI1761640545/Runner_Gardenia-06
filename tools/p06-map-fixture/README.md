# Synthetic offline map — CC0-1.0

The geometry in `gardenia-synthetic.osm` and the generated
`src/androidTest/assets/p06/gardenia-synthetic.map` is entirely self-authored,
fictional test data, dedicated to the public domain under CC0 1.0:
https://creativecommons.org/publicdomain/zero/1.0/
It is not extracted from OSM, private GPS data or any map provider and is not for navigation.

Generator: `GenerateMap.java` (CC0-1.0), Java17, Mapsforge writer/core0.25.0,
Osmosis core0.49.2, Guava28.1-jre, failureaccess1.0.1, Trove4j3.0.3, JTS core1.16.1.
Download these public Maven Central jars into a development-only directory,
compile the generator, and run `GenerateMap INPUT.osm OUTPUT.map`.
Writer dependencies are not application dependencies. The map writer embeds its
creation timestamp, so regeneration changes the binary digest; verify features
with `OfflineMapFixtureTest` rather than expecting reproducible writer bytes.

Checked-in fixture: 751 bytes, SHA-256 `e8c417146785e9a6e0874b94690a7ea269fde0a0ea926d9d6462864bc815a48f`.

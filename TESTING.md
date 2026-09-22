# Testing and test maintenance

This repository has two distinct automated-test surfaces: the shared client/common engine built with Gradle, and the Paper server plugin built with Maven. Keep tests beside the code they protect; Sentinel may consume built artifacts, but it is not the home for these handwritten regression tests.

## What is covered

### Common module

`common/src/test/java/net/enthusia/autoclicker/` covers configuration parsing, duration parsing, and core autoclicker engine behavior.

Run it with:

```bash
./gradlew :common:test
```

Gradle HTML results are under `common/build/reports/tests/test/` and XML results under `common/build/test-results/test/`.

### Server plugin

`server-plugin/src/test/java/net/enthusia/autoclicker/server/` covers click-session behavior, default targeting, client-handshake services, evidence retention and the raw handshake protocol. The test-hardening additions specifically exercise:

- `ClientEvidencePolicyTest`: positive retention/record bounds and fail-closed constructor validation;
- `HandshakePayloadCursorTest`: unsigned-byte decoding, VarInts, UTF-8 validation, blank/truncated/oversized fields and payload exhaustion;
- `ClientHandshakeParserTest`: valid evidence, unsupported protocols, oversized/empty/truncated/trailing payloads, blank fields and null-input behavior;
- `FullFeatureCoverageContractTest`: ensures critical common/server-plugin feature families remain backed by concrete regression files.

Run all server-plugin checks with:

```bash
cd server-plugin
mvn -B -ntp clean verify
mvn -B -ntp org.apache.maven.plugins:maven-pmd-plugin:3.26.0:check
```

Run one class with:

```bash
cd server-plugin
mvn -B -ntp -Dtest=ClientHandshakeParserTest test
```

Surefire reports are under `server-plugin/target/surefire-reports/`.

## CI

`.github/workflows/server-plugin.yml` runs for server-plugin changes. It executes Maven `clean verify`, PMD, verifies that the public client-evidence API is actually packaged, checks that test/Paper classes did not leak into the JAR, and uploads the verified server plugin artifact.

A green workflow means the checked-out PR head compiled and its automated server-plugin suite passed. It does **not** prove Fabric/Forge/NeoForge behavior in a real client or a real Paper server.

## How to review a failure

1. Confirm the workflow ran against the current PR head.
2. Identify whether failure is compilation, JUnit, PMD, packaging, or infrastructure/no-runner.
3. For JUnit failures, read the Surefire class/test name and reproduce with `-Dtest=ClassName`.
4. Do not weaken protocol limits, validation, PMD, assertions, or production guarantees just to make CI green.
5. If a test exposes a product defect, fix the product in the branch/work package that owns that production path; keep a regression test demonstrating the defect.

## Adding or changing features

When a production change affects a covered family, update or add behavioral tests in the same PR. If it introduces a new major feature family, add real tests first and then extend `FullFeatureCoverageContractTest` so later removals are visible.

## Important remaining boundaries

The current automated suite is strong around common logic and server-side handshake/evidence behavior, but it is not exhaustive across every loader/runtime. High-value future work includes:

- Fabric, Forge and NeoForge client initialization and input integration for every supported Minecraft-version module;
- shared-client networking behavior against actual loader implementations;
- Paper command/listener behavior requiring a server harness;
- CombatX integration and internal combat tracking in a realistic runtime;
- end-to-end client-to-server handshake compatibility across supported version/loader combinations;
- manual validation that clicks interact with Minecraft exactly as expected under latency, GUI, combat and disconnect conditions.

Those belong in loader-specific tests, a real client/server integration harness, or Sentinel/real-Paper compatibility checks as appropriate. Do not mock an external runtime into existence and call that proof of compatibility.

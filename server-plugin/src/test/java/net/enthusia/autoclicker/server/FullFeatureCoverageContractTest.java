package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Inventory guard for critical common/server-plugin behavior with concrete regression evidence. */
final class FullFeatureCoverageContractTest {
    @Test
    void criticalFeatureFamiliesRetainRegressionEvidence() {
        Path root = repositoryRoot();
        coverage().forEach((feature, paths) -> paths.forEach(relative ->
                assertTrue(Files.isRegularFile(root.resolve(relative)),
                        feature + " lost required regression evidence: " + relative)));
    }

    private static Map<String, List<String>> coverage() {
        Map<String, List<String>> coverage = new LinkedHashMap<>();
        coverage.put("common configuration and duration parsing", List.of(
                "common/src/test/java/net/enthusia/autoclicker/AutoclickerConfigTest.java",
                "common/src/test/java/net/enthusia/autoclicker/DurationParserTest.java"));
        coverage.put("common autoclicker engine behavior", List.of(
                "common/src/test/java/net/enthusia/autoclicker/AutoclickerEngineTest.java"));
        coverage.put("server click-session lifecycle", List.of(
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/AutoClickSessionTest.java"));
        coverage.put("handshake parsing and payload decoding", List.of(
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/ClientHandshakeParserTest.java",
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/HandshakePayloadCursorTest.java",
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/ClientHandshakeServiceTest.java"));
        coverage.put("bounded client evidence retention and validation", List.of(
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/ClientEvidencePolicyTest.java",
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/ClientEvidenceRetentionTest.java",
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/api/ClientEvidenceSnapshotTest.java"));
        coverage.put("public handshake API snapshot contract", List.of(
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/api/ClientHandshakeSnapshotTest.java"));
        coverage.put("default targeting configuration", List.of(
                "server-plugin/src/test/java/net/enthusia/autoclicker/server/DefaultTargetConfigurationTest.java"));
        return coverage;
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(current.resolve("server-plugin/pom.xml"))) {
            return current;
        }
        if (Files.isRegularFile(current.resolve("pom.xml"))
                && "server-plugin".equals(String.valueOf(current.getFileName()))) {
            return current.getParent();
        }
        Path parent = current.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("server-plugin/pom.xml"))) {
            return parent;
        }
        throw new IllegalStateException("Could not locate EnthusiaAutoClicker repository root from " + current);
    }
}

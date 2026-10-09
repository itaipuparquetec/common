package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditProperties;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditSinkType;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Confidential;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Internal;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Public;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Sensitive;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationOmitsUnannotatedTest {

    private static final String REDACTED = "***REDACTED***";
    private static final String PSEUDONYM_PREFIX = "anon:";

    @Test
    void shouldOmitFromTheTrailEveryFieldWithoutAnyClassificationAnnotation() throws Exception {
        final var trail = trailOf(sample(), allInClearText());

        assertThat(trail).doesNotContainKey("secret").doesNotContainKey("note");
        assertThat(trail.keySet()).containsExactlyInAnyOrder("name", "department", "document", "health");
    }

    @Test
    void shouldLogEachClassifiedFieldAccordingToItsStrategy() throws Exception {
        final var trail = trailOf(sample(), confidentialMasked());

        assertThat(trail).containsEntry("name", "Ada Lovelace").containsEntry("department", "Engineering").containsEntry("health", REDACTED);
        assertThat(trail.get("document")).asString().startsWith(PSEUDONYM_PREFIX);
    }

    @Test
    void shouldKeepSensitiveAlwaysRedactedRegardlessOfClearTextFlags() throws Exception {
        final var trail = trailOf(sample(), allInClearText());

        assertThat(trail).containsEntry("health", REDACTED);
    }

    @ParameterizedTest
    @CsvSource({"true,Ada Lovelace", "false," + REDACTED})
    void shouldHonorThePublicClearTextFlag(final boolean publicInClear, final String expected) throws Exception {
        final var trail = trailOf(sample(), properties(publicInClear, true, false));

        assertThat(trail).containsEntry("name", expected);
    }

    @ParameterizedTest
    @CsvSource({"true,Engineering", "false," + REDACTED})
    void shouldHonorTheInternalClearTextFlag(final boolean internalInClear, final String expected) throws Exception {
        final var trail = trailOf(sample(), properties(true, internalInClear, false));

        assertThat(trail).containsEntry("department", expected);
    }

    private Map<String, Object> trailOf(final Subject subject, final AuditProperties properties) throws Exception {
        final var masking = AuditMappers.masking(properties, new Pseudonymizer("test-salt"));
        final var json = masking.writeValueAsString(subject);
        return new ObjectMapper().readValue(json, new MapType());
    }

    private Subject sample() {
        return new Subject("Ada Lovelace", "Engineering", "12345678900", "allergic to penicillin",
                "top-secret", "just a note");
    }

    private AuditProperties allInClearText() {
        return properties(true, true, true);
    }

    private AuditProperties confidentialMasked() {
        return properties(true, true, false);
    }

    private AuditProperties properties(final boolean publicInClear, final boolean internalInClear,
                                       final boolean confidentialInClear) {
        return new AuditProperties(true, "test-salt",
                new AuditProperties.Cleartext(publicInClear, internalInClear, confidentialInClear),
                AuditSinkType.LOG, false, 32768, "test",
                new AuditProperties.Topics("events", "events.dlt"),
                new AuditProperties.Outbox(true, 2, 10000),
                new AuditProperties.Relay(false, Duration.ofSeconds(1), 100, Duration.ofSeconds(30),
                        Duration.ofSeconds(60), 10));
    }

    private record Subject(@Public String name, @Internal String department, @Confidential String document,
                           @Sensitive String health, String secret, String note) {
    }

    private static final class MapType extends com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> {
    }
}

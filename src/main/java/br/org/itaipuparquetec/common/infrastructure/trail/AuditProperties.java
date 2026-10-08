package br.org.itaipuparquetec.common.infrastructure.trail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

import java.time.Duration;

/**
 * Audit trail configuration, bound from the environment (e.g. via {@code .env}).
 *
 * <p>Sensitive data is never rendered in clear text regardless of these flags.</p>
 *
 * <ul>
 *   <li>{@code AUDIT_ENABLED} → {@code audit.enabled}</li>
 *   <li>{@code AUDIT_SINK} → {@code audit.sink} ({@code log} by default, {@code outbox} for guaranteed delivery)</li>
 *   <li>{@code AUDIT_INCLUDE_READ_ONLY} → {@code audit.include-read-only} (default {@code false})</li>
 *   <li>{@code AUDIT_MAX_INPUT_BYTES} → {@code audit.max-input-bytes} (default 32768)</li>
 *   <li>{@code AUDIT_SOURCE_VERSION} → {@code audit.source-version} (used when no build info is available)</li>
 *   <li>{@code AUDIT_CLEARTEXT_PUBLIC} → {@code audit.cleartext.public}</li>
 *   <li>{@code AUDIT_CLEARTEXT_INTERNAL} → {@code audit.cleartext.internal}</li>
 *   <li>{@code AUDIT_CLEARTEXT_CONFIDENTIAL} → {@code audit.cleartext.confidential}</li>
 *   <li>{@code AUDIT_PSEUDONYMIZATION_SALT} → {@code audit.pseudonymization-salt}</li>
 *   <li>{@code audit.topics.events} / {@code audit.topics.dead-letter}: Kafka topics of the trail</li>
 *   <li>{@code audit.outbox.*}: schema migration and the deferred writers of the outbox</li>
 *   <li>{@code audit.relay.*}: outbox relay (disabled by default)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "audit")
public record AuditProperties(
        @DefaultValue("true") boolean enabled,
        String pseudonymizationSalt,
        @DefaultValue Cleartext cleartext,
        @DefaultValue("LOG") AuditSinkType sink,
        @DefaultValue("false") boolean includeReadOnly,
        @DefaultValue("32768") int maxInputBytes,
        String sourceVersion,
        @DefaultValue Topics topics,
        @DefaultValue Outbox outbox,
        @DefaultValue Relay relay) {

    /**
     * Per-category clear-text flags. When a category is not allowed in clear text, confidential data is
     * pseudonymized and every other non-public category is redacted.
     */
    public record Cleartext(
            @Name("public") @DefaultValue("true") boolean publicData,
            @DefaultValue("true") boolean internal,
            @DefaultValue("false") boolean confidential) {
    }

    /**
     * Kafka topics of the trail. The dead-letter topic is consumed/produced by the trail consumer; it is exposed
     * here so producers and consumers share the same configuration key.
     */
    public record Topics(
            @DefaultValue("hubti.trail.events") String events,
            @DefaultValue("hubti.trail.events.dlt") String deadLetter) {
    }

    /**
     * Outbox management.
     *
     * @param migrateSchema         whether the starter creates the {@code audit_outbox} table at startup
     * @param deferredWriterThreads threads that write, after the business transaction ends, the events of failures
     *                              and read-only executions
     * @param deferredQueueCapacity events waiting for those threads; beyond it, events fall back to the log
     */
    public record Outbox(
            @DefaultValue("true") boolean migrateSchema,
            @DefaultValue("2") int deferredWriterThreads,
            @DefaultValue("10000") int deferredQueueCapacity) {
    }

    /**
     * Relay that publishes the outbox rows to Kafka.
     *
     * @param enabled       whether this instance runs the relay
     * @param interval      pause between two relay cycles
     * @param batchSize     maximum rows locked and published per tenant per cycle
     * @param sendTimeout   maximum time waiting for the broker acknowledgement of a batch
     * @param tenantRefresh how often the list of tenants is refreshed
     * @param alertAttempts publish attempts of a single row that raise an alert
     */
    public record Relay(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("1s") Duration interval,
            @DefaultValue("100") int batchSize,
            @DefaultValue("30s") Duration sendTimeout,
            @DefaultValue("60s") Duration tenantRefresh,
            @DefaultValue("10") int alertAttempts) {
    }

    public MaskingStrategy resolveStrategy(final DataClassification classification) {
        return switch (classification) {
            case PUBLIC -> cleartext.publicData() ? MaskingStrategy.NONE : MaskingStrategy.REDACT;
            case INTERNAL -> cleartext.internal() ? MaskingStrategy.NONE : MaskingStrategy.REDACT;
            case CONFIDENTIAL -> cleartext.confidential() ? MaskingStrategy.NONE : MaskingStrategy.PSEUDONYMIZE;
            case SENSITIVE -> MaskingStrategy.REDACT;
        };
    }

    public boolean migratesOutboxSchema() {
        return enabled && sink == AuditSinkType.OUTBOX && outbox.migrateSchema();
    }
}

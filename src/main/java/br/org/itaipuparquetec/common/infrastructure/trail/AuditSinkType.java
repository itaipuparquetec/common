package br.org.itaipuparquetec.common.infrastructure.trail;

/**
 * Selects where the audit events are delivered ({@code audit.sink}).
 */
public enum AuditSinkType {
    /** Structured log under the {@code AUDIT} logger. Draft delivery, with no delivery guarantee. */
    LOG,
    /** Transactional outbox in the service database, published to Kafka by the relay (ADR-031). */
    OUTBOX
}

package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.UUID;

/**
 * Row of the {@code audit_outbox} table: the serialized envelope plus the values needed to build the Kafka
 * record (key and headers) without parsing the payload again.
 */
public record AuditOutboxEntry(
        UUID eventId,
        String schemaVersion,
        String tenant,
        String sourceService,
        String traceId,
        String spanId,
        String payload) {
}

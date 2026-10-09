package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.util.UUID;

/**
 * Row read from the outbox by the relay.
 */
public record AuditOutboxRow(
        UUID eventId,
        String schemaVersion,
        String tenant,
        String sourceService,
        String traceId,
        String spanId,
        String payload,
        int attempts) {
}

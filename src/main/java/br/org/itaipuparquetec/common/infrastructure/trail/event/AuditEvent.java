package br.org.itaipuparquetec.common.infrastructure.trail.event;

import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActorType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Structured audit event built for each use case execution. The {@code input} body is serialized with the
 * classification-aware object mapper, so sensitive data is masked centrally. The use case output is never part of
 * the event (data minimization, ADR-021).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
        UUID eventId,
        Instant occurredAt,
        long durationMillis,
        String tenant,
        String actorTenant,
        String sourceService,
        String sourceVersion,
        String traceId,
        String spanId,
        String parentSpanId,
        String useCase,
        String useCaseVersion,
        String actor,
        AuditActorType actorType,
        String sid,
        String jti,
        AuditResult result,
        AuditError error,
        Object input) {
}

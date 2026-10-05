package br.org.itaipuparquetec.common.infrastructure.audit.trail.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Structured audit event built for each use case execution. The {@code input} and {@code output} bodies are
 * serialized with the classification-aware object mapper, so sensitive data is masked centrally.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
        String useCase,
        String version,
        String actor,
        String sid,
        String jti,
        String traceId,
        Instant timestamp,
        long durationMillis,
        AuditResult result,
        Object input,
        Object output,
        AuditError error) {
}

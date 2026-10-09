package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Trail event envelope, schema version 1 (contract {@code trail-event-v1.schema.json}). It is the payload
 * published to Kafka; it declares no collection. The use case output and the exception message are never part
 * of it (data minimization, ADR-021).
 *
 * @param input        the masked input of the use case as a serialized JSON document, opaque to the trail
 * @param inputOmitted reason why the input is absent, when it was dropped
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrailEventEnvelope(
        String schemaVersion,
        String eventId,
        String occurredAt,
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
        String actorType,
        String sid,
        String jti,
        String result,
        Error error,
        String input,
        String inputOmitted) {

    public static final String SCHEMA_VERSION = "1";

    /**
     * Failure data: simple exception type name and category ({@code BUSINESS} or {@code TECHNICAL}).
     */
    public record Error(String type, String category) {
    }
}

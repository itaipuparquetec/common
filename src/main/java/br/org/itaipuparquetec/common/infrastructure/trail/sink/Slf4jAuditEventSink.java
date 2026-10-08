package br.org.itaipuparquetec.common.infrastructure.trail.sink;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink that logs the audit event as classification-masked JSON under a dedicated {@code AUDIT} logger. It is the
 * selected sink when {@code audit.sink=log} (no delivery guarantee) and the fallback of the outbox sink when a row
 * cannot be written (ADR-031).
 */
public class Slf4jAuditEventSink implements AuditEventSink {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper auditObjectMapper;

    public Slf4jAuditEventSink(final ObjectMapper auditObjectMapper) {
        this.auditObjectMapper = auditObjectMapper;
    }

    @Override
    public void publish(final AuditEvent event) {
        try {
            final var exceptionMessage = auditObjectMapper.writeValueAsString(event);
            AUDIT.info(exceptionMessage);
        } catch (final JsonProcessingException exception) {
            AUDIT.error("Could not serialize the audit event of use case {}", event.useCase(), exception);
        }
    }
}

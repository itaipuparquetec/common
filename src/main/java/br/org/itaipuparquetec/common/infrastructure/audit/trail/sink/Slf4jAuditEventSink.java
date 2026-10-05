package br.org.itaipuparquetec.common.infrastructure.audit.trail.sink;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.event.AuditEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draft sink that logs the audit event as classification-masked JSON under a dedicated {@code AUDIT} logger.
 * The production delivery (outbox in the use case transaction + Kafka with acks=all and idempotence) replaces
 * this implementation without touching the aspect.
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

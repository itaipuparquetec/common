package br.org.itaipuparquetec.common.infrastructure.audit.trail.sink;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.event.AuditEvent;

/**
 * Destination of audit events. The target delivery is asynchronous and guaranteed (outbox + Kafka), but this
 * contract is agnostic to the transport.
 */
public interface AuditEventSink {
    void publish(AuditEvent event);
}

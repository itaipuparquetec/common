package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.UUID;

/**
 * The envelope of an audit event could not be serialized to JSON.
 */
public class AuditEnvelopeSerializationException extends RuntimeException {

    public AuditEnvelopeSerializationException(final UUID eventId, final Throwable cause) {
        super("Cannot serialize the envelope of audit event '" + eventId + "' to a JSON document", cause);
    }
}

package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.envelope.TrailEventEnvelope;
import br.org.itaipuparquetec.common.infrastructure.trail.envelope.TrailEventEnvelopeFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Turns an {@link AuditEvent} into the {@link AuditOutboxEntry} that is stored: the masked, serialized envelope.
 */
public class AuditOutboxEntryFactory {

    private final TrailEventEnvelopeFactory envelopes;
    private final ObjectMapper envelopeMapper;

    public AuditOutboxEntryFactory(final TrailEventEnvelopeFactory envelopes, final ObjectMapper envelopeMapper) {
        this.envelopes = envelopes;
        this.envelopeMapper = envelopeMapper;
    }

    public AuditOutboxEntry entryOf(final AuditEvent event) {
        final var envelope = envelopes.envelopeOf(event);
        return new AuditOutboxEntry(
                event.eventId(),
                envelope.schemaVersion(),
                envelope.tenant(),
                envelope.sourceService(),
                envelope.traceId(),
                envelope.spanId(),
                payloadOf(event, envelope));
    }

    private String payloadOf(final AuditEvent event, final TrailEventEnvelope envelope) {
        try {
            return envelopeMapper.writeValueAsString(envelope);
        } catch (final JsonProcessingException failure) {
            throw new AuditEnvelopeSerializationException(event.eventId(), failure);
        }
    }
}

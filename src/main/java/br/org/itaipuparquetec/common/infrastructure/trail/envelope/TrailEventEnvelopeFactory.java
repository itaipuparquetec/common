package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditError;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Builds the {@link TrailEventEnvelope} of an {@link AuditEvent}. The input is serialized with the
 * classification-aware mapper (masking happens here, at the origin) and dropped when it exceeds the size limit.
 */
public class TrailEventEnvelopeFactory {

    private static final DateTimeFormatter OCCURRED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final ObjectMapper maskingMapper;
    private final int maxInputBytes;

    public TrailEventEnvelopeFactory(final ObjectMapper maskingMapper, final int maxInputBytes) {
        this.maskingMapper = maskingMapper;
        this.maxInputBytes = maxInputBytes;
    }

    public TrailEventEnvelope envelopeOf(final AuditEvent event) {
        final var maskedInput = maskedInputOf(event.input());
        return new TrailEventEnvelope(
                TrailEventEnvelope.SCHEMA_VERSION,
                event.eventId().toString(),
                OCCURRED_AT_FORMAT.format(event.occurredAt()),
                event.durationMillis(),
                event.tenant(),
                event.actorTenant(),
                event.sourceService(),
                event.sourceVersion(),
                event.traceId(),
                event.spanId(),
                event.parentSpanId(),
                event.useCase(),
                event.useCaseVersion(),
                event.actor(),
                event.actorType().name(),
                event.sid(),
                event.jti(),
                event.result().name(),
                errorOf(event.error()),
                maskedInput.json(),
                maskedInput.omittedReason());
    }

    private static TrailEventEnvelope.Error errorOf(final AuditError error) {
        if (error == null) {
            return null;
        }
        return new TrailEventEnvelope.Error(error.type(), error.category().name());
    }

    private MaskedInput maskedInputOf(final Object input) {
        if (input == null) {
            return MaskedInput.absent();
        }
        try {
            return limited(maskingMapper.writeValueAsString(input));
        } catch (final JsonProcessingException failure) {
            return MaskedInput.omitted(InputOmittedReason.SERIALIZATION_ERROR);
        }
    }

    private MaskedInput limited(final String json) {
        if (json.getBytes(StandardCharsets.UTF_8).length > maxInputBytes) {
            return MaskedInput.omitted(InputOmittedReason.SIZE_LIMIT);
        }
        return new MaskedInput(json, null);
    }

    private record MaskedInput(String json, String omittedReason) {

        static MaskedInput absent() {
            return new MaskedInput(null, null);
        }

        static MaskedInput omitted(final InputOmittedReason reason) {
            return new MaskedInput(null, reason.name());
        }
    }
}

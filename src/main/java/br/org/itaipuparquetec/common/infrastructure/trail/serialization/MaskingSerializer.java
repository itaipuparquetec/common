package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.trail.MaskingStrategy;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializes a classified property either as a fixed placeholder (redaction) or as a deterministic pseudonym,
 * covering the whole value even when it is a collection or a map.
 */
public class MaskingSerializer extends JsonSerializer<Object> {

    private static final String REDACTED = "***REDACTED***";

    private final MaskingStrategy strategy;
    private final Pseudonymizer pseudonymizer;

    public MaskingSerializer(final MaskingStrategy strategy, final Pseudonymizer pseudonymizer) {
        this.strategy = strategy;
        this.pseudonymizer = pseudonymizer;
    }

    @Override
    public void serialize(final Object value, final JsonGenerator generator, final SerializerProvider serializers)
            throws IOException {
        if (strategy == MaskingStrategy.PSEUDONYMIZE) {
            generator.writeString(pseudonymizer.pseudonymize(value));
            return;
        }
        generator.writeString(REDACTED);
    }
}

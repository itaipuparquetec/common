package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Creates the object mappers of the audit trail. They are deliberately not beans: exposing them would make Spring
 * Boot back off its own {@code ObjectMapper} or leak the audit masking into the HTTP responses of the consumer.
 */
public final class AuditMappers {

    private AuditMappers() {
    }

    /**
     * Mapper that masks every value according to its data classification (ADR-021).
     *
     * @param properties    the audit configuration
     * @param pseudonymizer the pseudonymizer of confidential values
     * @return a new mapper
     */
    public static ObjectMapper masking(final AuditProperties properties, final Pseudonymizer pseudonymizer) {
        final var mapper = JsonMapper.builder().findAndAddModules().build();
        mapper.registerModule(new ClassificationMaskingModule(properties, pseudonymizer));
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * Mapper for the already masked envelope: it applies no masking.
     *
     * @return a new mapper
     */
    public static ObjectMapper envelope() {
        return JsonMapper.builder().build();
    }
}

package br.org.itaipuparquetec.common.infrastructure.audit.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.AuditProperties;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module that applies the ADR-021 classification masking to any serialized object graph.
 */
public class ClassificationMaskingModule extends SimpleModule {

    public ClassificationMaskingModule(final AuditProperties properties, final Pseudonymizer pseudonymizer) {
        super("ClassificationMaskingModule");
        setSerializerModifier(new ClassificationBeanSerializerModifier(properties, pseudonymizer));
    }
}

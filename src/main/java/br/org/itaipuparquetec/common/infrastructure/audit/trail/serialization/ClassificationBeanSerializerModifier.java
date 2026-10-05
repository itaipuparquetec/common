package br.org.itaipuparquetec.common.infrastructure.audit.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.AuditProperties;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.DataClassification;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.MaskingStrategy;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.annotation.Confidential;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.annotation.Internal;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.annotation.Public;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.annotation.Sensitive;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.List;

/**
 * Wraps every classified property with a {@link MaskingSerializer}. Because Jackson serializes nested records,
 * collections and maps recursively, the classification of each component is honored at any depth.
 */
public class ClassificationBeanSerializerModifier extends BeanSerializerModifier {

    private final transient AuditProperties properties;
    private final transient Pseudonymizer pseudonymizer;

    public ClassificationBeanSerializerModifier(final AuditProperties properties, final Pseudonymizer pseudonymizer) {
        this.properties = properties;
        this.pseudonymizer = pseudonymizer;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(final SerializationConfig config,
                                                     final BeanDescription beanDescription,
                                                     final List<BeanPropertyWriter> beanProperties) {
        for (final BeanPropertyWriter writer : beanProperties) {
            maskWhenClassified(writer);
        }
        return beanProperties;
    }

    private void maskWhenClassified(final BeanPropertyWriter writer) {
        final var classification = classificationOf(writer.getMember());
        if (classification == null) {
            return;
        }
        final var strategy = properties.resolveStrategy(classification);
        if (strategy != MaskingStrategy.NONE) {
            writer.assignSerializer(new MaskingSerializer(strategy, pseudonymizer));
        }
    }

    private static DataClassification classificationOf(final AnnotatedMember member) {
        if (member == null) {
            return null;
        }
        if (member.hasAnnotation(Sensitive.class)) {
            return DataClassification.SENSITIVE;
        }
        if (member.hasAnnotation(Confidential.class)) {
            return DataClassification.CONFIDENTIAL;
        }
        if (member.hasAnnotation(Internal.class)) {
            return DataClassification.INTERNAL;
        }
        if (member.hasAnnotation(Public.class)) {
            return DataClassification.PUBLIC;
        }
        return null;
    }
}

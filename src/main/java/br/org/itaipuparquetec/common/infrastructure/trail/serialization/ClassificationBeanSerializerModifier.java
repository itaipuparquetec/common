package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditProperties;
import br.org.itaipuparquetec.common.infrastructure.trail.DataClassification;
import br.org.itaipuparquetec.common.infrastructure.trail.MaskingStrategy;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Confidential;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Internal;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Public;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Sensitive;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps in the audit trail only the properties that carry a classification annotation
 * ({@link Public}, {@link Internal}, {@link Confidential} or {@link Sensitive}) and wraps each one with a
 * {@link MaskingSerializer} according to its strategy. A property without any classification annotation is
 * omitted entirely: nothing of it reaches the trail. Because Jackson serializes nested records, collections and
 * maps recursively, this rule is honored at any depth.
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
        final List<BeanPropertyWriter> classifiedProperties = new ArrayList<>(beanProperties.size());
        for (final BeanPropertyWriter writer : beanProperties) {
            final var classification = classificationOf(writer.getMember());
            if (classification == null) {
                continue;
            }
            maskWhenNeeded(writer, classification);
            classifiedProperties.add(writer);
        }
        return classifiedProperties;
    }

    private void maskWhenNeeded(final BeanPropertyWriter writer, final DataClassification classification) {
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

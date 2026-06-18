package br.org.itaipuparquetec.common;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

class ArchUnitMethodIgnoreTest {

    @Test
    void mustBeAnAnnotationType() {
        assertThat(ArchUnitMethodIgnore.class.isAnnotation()).isTrue();
    }

    @Test
    void mustTargetOnlyMethods() {
        final var target = ArchUnitMethodIgnore.class.getAnnotation(Target.class);

        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

    @Test
    void mustNotHaveExplicitRetentionPolicy() {
        final var retention = ArchUnitMethodIgnore.class.getAnnotation(Retention.class);

        assertThat(retention).isNull();
    }

    /**
     * This method is empty to test annotation ArchUnitMethodIgnore.
     */
    @ArchUnitMethodIgnore
    void annotatedMethod() {
    }

    @Test
    void mustBeApplicableToMethods() throws NoSuchMethodException {
        final var method = ArchUnitMethodIgnoreTest.class.getDeclaredMethod("annotatedMethod");

        assertThat(method).isNotNull();
    }
}
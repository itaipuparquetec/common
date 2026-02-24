package br.org.itaipuparquetec.common.infrastructure.aid;

import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandaloneBeanValidationTest {

    @Test
    void cannotThrowAnyExceptionIfTheModelDoesntHaveInvalidations() {
        final var jpaModel = new JpaModelExample(UUID.randomUUID(), "Valid Name", "Valid Description");

        assertDoesNotThrow(() -> StandaloneBeanValidation.validate(jpaModel));
    }

    @Test
    void mustThrowAnExceptionIfTheModelHaveInvalidations() {
        final var jpaModel = new JpaModelExample(UUID.randomUUID(), "Big name Big name Big name Big name",
                "Big Description Big Description Big Description Big Description Big Description " +
                        "Big Description Big Description Big Description Big Description Big Description " +
                        "Big Description Big Description Big Description Big Description Big Description " +
                        "Big Description Big Description Big Description Big Description Big Description");

        final var exception = assertThrows(ConstraintViolationException.class,
                () -> StandaloneBeanValidation.validate(jpaModel));

        Assertions.assertThat(exception.getMessage())
                .contains("description: Description must be between 1 and 255 characters",
                        "name: Name must be between 1 and 32 characters");
    }
}

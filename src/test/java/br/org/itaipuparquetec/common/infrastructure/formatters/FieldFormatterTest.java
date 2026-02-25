package br.org.itaipuparquetec.common.infrastructure.formatters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldFormatterTest {

    @Test
    void cannotInstantiateFieldFormatterByDefaultConstructor() {
        final var exception = assertThrows(IllegalStateException.class, FieldFormatter::new);

        assertThat(exception.getMessage()).isEqualTo("Utility class");
    }

    @ParameterizedTest
    @MethodSource("provideCustomFieldsToFormatThem")
    void mustFormat(final String expected, final String conjunction, String... fields) {
        final var result = FieldFormatter.format(conjunction, fields);

        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideCustomFieldsToFormatThem() {
        return Stream.of(
                Arguments.of("", "e", null),
                Arguments.of("", "and", null),
                Arguments.of("", "e", new String[]{}),
                Arguments.of("", "and", new String[]{}),
                Arguments.of("campo1", "e", new String[]{"campo1"}),
                Arguments.of("field1", "and", new String[]{"field1"}),
                Arguments.of("campo1 e campo2", "e", new String[]{"campo1", "campo2"}),
                Arguments.of("field1 and field2", "and", new String[]{"field1", "field2"}),
                Arguments.of("campo1, campo2 e campo3", "e", new String[]{"campo1", "campo2", "campo3"}),
                Arguments.of("field1, field2 and field3", "and", new String[]{"field1", "field2", "field3"}),
                Arguments.of("field1, field1 and field1", "and", new String[]{"field1", "field1", "field1"})
        );
    }
}
package br.org.itaipuparquetec.common.infrastructure.rest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class RestResponseEntityExceptionHandlerTest {

    @ParameterizedTest
    @MethodSource("provideCustomMessagesToExtractParameters")
    void musExtractAttributesFromCustomMessage(final String customMessage, final String expectedAttributes) {
        final var extractedAttributes = RestResponseEntityExceptionHandler.extractAttributeFromMessage(customMessage);

        Assertions.assertThat(extractedAttributes).isEqualTo(expectedAttributes);
    }

    private static Stream<Arguments> provideCustomMessagesToExtractParameters() {
        return Stream.of(
                Arguments.of("Field 'username' must not be null", "username"),
                Arguments.of("Field 'password' must not be null", "password"),
                Arguments.of("Field 'email' must not be null", "email"),
                Arguments.of("\"Attribute1\",\"Attribute2\",\"Attribute3\"", "Attribute1,Attribute2,Attribute3"),
                Arguments.of("   \"Attribute1\",   \"Attribute2\",   \"Attribute3\"   ", "Attribute1,Attribute2,Attribute3"),
                Arguments.of("   \"Attribute1\", message  \"Attribute2\", message \"Attribute3\"   ", "Attribute1,Attribute2,Attribute3")
        );
    }
}

package br.org.itaipuparquetec.common.infrastructure.rest;

import br.org.itaipuparquetec.common.application.services.LocaleService;
import br.org.itaipuparquetec.common.domain.exceptions.AlreadyExistsFieldsException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class RestResponseEntityExceptionHandlerTest {

    private final MessageSource messageSource = mock(MessageSource.class);
    private final LocaleService localeService = mock(LocaleService.class);

    private final RestResponseEntityExceptionHandler restResponseEntityExceptionHandler =
            spy(new RestResponseEntityExceptionHandler(messageSource, localeService));

    @Test
    void mustHandleAlreadyExistsFieldsException() {
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        final String errorMessageExpected = "Os campos campo1 e campo2 já existem.";
        final String keyCodeOfMessageExpected = "repository.fieldsAlreadyExists";
        final String joinedFieldsExpected = "field1,field2";
        final String[] splitFieldsExpected = new String[]{"field1", "field2"};
        final String translatedFieldsExpected = "campo1,campo2";
        final String[] splitTranslatedFieldsExpected = new String[]{"campo1", "campo2"};
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(joinedFieldsExpected), any(String[].class), eq(DEFAULT_LOCALE)))
                .thenReturn(translatedFieldsExpected);
        when(messageSource.getMessage(keyCodeOfMessageExpected, splitTranslatedFieldsExpected, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var exception = new AlreadyExistsFieldsException(splitFieldsExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleAlreadyExistsFieldsException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq(joinedFieldsExpected), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, splitTranslatedFieldsExpected, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

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

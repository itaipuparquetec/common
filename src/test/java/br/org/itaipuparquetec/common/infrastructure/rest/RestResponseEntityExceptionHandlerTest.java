package br.org.itaipuparquetec.common.infrastructure.rest;

import br.org.itaipuparquetec.common.application.services.LocaleService;
import br.org.itaipuparquetec.common.domain.exceptions.*;
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
        final String errorMessageExpected = "Os campos campo1, campo2 e campo3 já existem.";
        final String keyCodeOfMessageExpected = "repository.fieldsAlreadyExists";
        final String[] splitFieldsExpected = new String[]{"field1", "field2", "field3"};
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        when(messageSource.getMessage(eq("field2"), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo2");
        when(messageSource.getMessage(eq("and"), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("e");
        when(messageSource.getMessage(eq("field3"), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo3");
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1, campo2 e campo3"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var exception = new AlreadyExistsFieldsException(splitFieldsExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(eq("field2"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(eq("field3"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1, campo2 e campo3"}, DEFAULT_LOCALE);
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

    @Test
    void mustHandleEmptyFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldCannotBeEmpty";
        final var exception = new EmptyFieldException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleAlreadyExistsFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldAlreadyExists";
        final var exception = new AlreadyExistsFieldException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleNullFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldCannotBeNull";
        final var exception = new NullFieldException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleLessThanZeroFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldCannotBeLessThanZero";
        final var exception = new LessThanZeroFieldException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleTooLargeFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldCannotBeGreaterThan";
        final var exception = new TooLargeFieldException(attribute, 100);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute + ",100"), any(String[].class), eq(DEFAULT_LOCALE)))
                .thenReturn("campo1", "100");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1,100"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleTooShortFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldCannotBeLessThan";
        final var exception = new TooShortFieldException(attribute, 100);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute + ",100"), any(String[].class), eq(DEFAULT_LOCALE)))
                .thenReturn("campo1", "100");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1,100"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleInvalidFieldException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.fieldIsInvalid";
        final var exception = new InvalidFieldException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleNotFoundRegisterException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "repository.notFoundRegister";
        final var exception = new NotFoundRegisterException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.BAD_REQUEST), eq(webRequestExpected));
    }

    @Test
    void mustHandleForbiddenException() {
        final String attribute = "field1";
        final String keyCodeOfMessageExpected = "security.accessDeniedToResource";
        final var exception = new ForbiddenException(attribute);
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(messageSource.getMessage(eq(attribute), any(String[].class), eq(DEFAULT_LOCALE))).thenReturn("campo1");
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(eq("field1"), any(String[].class), eq(DEFAULT_LOCALE));
        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{"campo1"}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.FORBIDDEN), eq(webRequestExpected));
    }

    @Test
    void mustHandleAccessDeniedException() {
        final String keyCodeOfMessageExpected = "security.accessDenied";
        final var exception = new org.springframework.security.access.AccessDeniedException("Access is denied");
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleException(exception, webRequestExpected);

        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.FORBIDDEN), eq(webRequestExpected));
    }

    @Test
    void mustHandleGenericException() {
        final String keyCodeOfMessageExpected = "repository.genericException";
        final var exception = new Exception("Generic Exception");
        final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
        when(localeService.getLocale()).thenReturn(DEFAULT_LOCALE);
        final String errorMessageExpected = "Error expected.";
        when(messageSource.getMessage(keyCodeOfMessageExpected, new String[]{}, DEFAULT_LOCALE))
                .thenReturn(errorMessageExpected);
        final var webRequestExpected = Mockito.mock(WebRequest.class);

        restResponseEntityExceptionHandler.handleGenericException(exception, webRequestExpected);

        verify(messageSource).getMessage(keyCodeOfMessageExpected, new String[]{}, DEFAULT_LOCALE);
        verify(restResponseEntityExceptionHandler).handleExceptionInternal(eq(exception), eq(errorMessageExpected),
                any(HttpHeaders.class), eq(HttpStatus.INTERNAL_SERVER_ERROR), eq(webRequestExpected));
    }
}

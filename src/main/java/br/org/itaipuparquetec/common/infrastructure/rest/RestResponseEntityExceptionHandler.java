package br.org.itaipuparquetec.common.infrastructure.rest;

import br.org.itaipuparquetec.common.application.services.LocaleService;
import br.org.itaipuparquetec.common.domain.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Order(9999)
@RestControllerAdvice
@RequiredArgsConstructor
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    private final MessageSource messageSource;
    private final LocaleService localeService;

    @ExceptionHandler(EmptyFieldException.class)
    public ResponseEntity<Object> handleEmptyFieldException(final EmptyFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldCannotBeEmpty", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AlreadyExistsFieldException.class)
    public ResponseEntity<Object> handleAlreadyExistsFieldException(final AlreadyExistsFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldAlreadyExists", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AlreadyExistsFieldsException.class)
    public ResponseEntity<Object> handleAlreadyExistsFieldsException(final AlreadyExistsFieldsException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var messageOfError = messageSource.getMessage("repository.fieldsAlreadyExists", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(messageOfError, exception);
        return handleExceptionInternal(exception, messageOfError, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NullFieldException.class)
    public ResponseEntity<Object> handleNullFieldException(final NullFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldCannotBeNull", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(LessThanZeroFieldException.class)
    public ResponseEntity<Object> handleLessThanZeroFieldException(final LessThanZeroFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldCannotBeLessThanZero", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(TooLargeFieldException.class)
    public ResponseEntity<Object> handleTooLargeFieldException(final TooLargeFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldCannotBeGreaterThan", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(TooShortFieldException.class)
    public ResponseEntity<Object> handleTooShortFieldException(final TooShortFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldCannotBeLessThan", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidFieldException.class)
    public ResponseEntity<Object> handleInvalidFieldException(final InvalidFieldException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.fieldIsInvalid", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NotFoundRegisterException.class)
    public ResponseEntity<Object> handleNotFoundRegisterException(final NotFoundRegisterException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("repository.notFoundRegister", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(final ForbiddenException exception, final WebRequest request) {
        final var attributes = messageSource.getMessage(extractAttributeFromMessage(exception.getMessage()), new String[]{},
                localeService.getLocale());
        final var message = messageSource.getMessage("security.accessDeniedToResource", attributes.split(","),
                localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(final org.springframework.security.access.AccessDeniedException exception,
                                                              final WebRequest request) {
        final var message = this.messageSource.getMessage("security.accessDenied", null, localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(final Exception exception, final WebRequest request) {
        final var message = messageSource.getMessage("repository.genericException", null, localeService.getLocale());
        LOGGER.error(message, exception);
        return handleExceptionInternal(exception, new Error(message), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    public ResponseEntity<Object> handleExceptionInternal(Exception exception, String messageOfError, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest webRequest) {
        return super.handleExceptionInternal(exception, new Error(messageOfError), headers, statusCode, webRequest);
    }

    static String extractAttributeFromMessage(final String message) {
        final var pattern = Pattern.compile("[\"']([^\"']+)[\"']");
        final var matcher = pattern.matcher(message);
        final List<String> atributos = new ArrayList<>();
        while (matcher.find()) {
            atributos.add(matcher.group(1));
        }
        return String.join(",", atributos);
    }

    public record Error(String message) {

    }
}

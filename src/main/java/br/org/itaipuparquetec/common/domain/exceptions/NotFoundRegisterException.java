package br.org.itaipuparquetec.common.domain.exceptions;

public class NotFoundRegisterException extends RuntimeException {
    static final String NOT_FOUND_REGISTER_MESSAGE = "Not found register \"%s\".";

    public NotFoundRegisterException(final String field) {
        super(NOT_FOUND_REGISTER_MESSAGE.formatted(field));
    }
}

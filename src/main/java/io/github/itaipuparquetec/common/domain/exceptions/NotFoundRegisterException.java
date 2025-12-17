package io.github.itaipuparquetec.common.domain.exceptions;


public class NotFoundRegisterException extends RuntimeException {
    final static String NOT_FOUND_REGISTER_MESSAGE = "Not found register \"%s\".";

    public NotFoundRegisterException(String field) {
        super(NOT_FOUND_REGISTER_MESSAGE.formatted(field));
    }
}

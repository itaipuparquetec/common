package io.github.itaipuparquetec.common.domain.exceptions;

public class InvalidFieldException extends RuntimeException {

    final static String INVALID_FIELD_MESSAGE = "The field \"%s\" is invalid.";

    public InvalidFieldException(String field) {
        super(INVALID_FIELD_MESSAGE.formatted(field));
    }
}

package io.github.itaipuparquetec.common.domain.exceptions;

public class AlreadyExistsFieldException extends RuntimeException {
    final static String ALREADY_EXITS_MESSAGE = "The field \"%s\" already registered.";

    public AlreadyExistsFieldException(String field) {
        super(ALREADY_EXITS_MESSAGE.formatted(field));
    }
}

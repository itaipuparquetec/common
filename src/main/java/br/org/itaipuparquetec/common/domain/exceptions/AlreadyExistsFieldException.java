package br.org.itaipuparquetec.common.domain.exceptions;

public class AlreadyExistsFieldException extends RuntimeException {
    static final String ALREADY_EXITS_FIELD_MESSAGE = "The field \"%s\" already registered.";

    public AlreadyExistsFieldException(String field) {
        super(ALREADY_EXITS_FIELD_MESSAGE.formatted(field));
    }
}

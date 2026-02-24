package br.org.itaipuparquetec.common.domain.exceptions;

public class EmptyFieldException extends RuntimeException {

    static final String NOT_EMPTY_MESSAGE = "The field \"%s\" cannot be empty.";

    public EmptyFieldException(String field) {
        super(NOT_EMPTY_MESSAGE.formatted(field));
    }
}

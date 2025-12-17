package br.org.itaipuparquetec.common.domain.exceptions;

public class NullFieldException extends RuntimeException {
    final static String NOT_NULL_MESSAGE = "The field \"%s\" cannot be null.";

    public NullFieldException(String field) {
        super(NOT_NULL_MESSAGE.formatted(field));
    }
}

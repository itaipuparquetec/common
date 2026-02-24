package br.org.itaipuparquetec.common.domain.exceptions;

public class TooLargeFieldException extends RuntimeException {

    static final String TO_LARGE_FIELD_MESSAGE = "The max size of the field \"%s\" is \"%s\".";

    public TooLargeFieldException(String field, int maxSize) {
        super(TO_LARGE_FIELD_MESSAGE.formatted(field, maxSize));
    }
}

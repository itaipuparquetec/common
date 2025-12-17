package io.github.itaipuparquetec.common.domain.exceptions;


public class TooShortFieldException extends RuntimeException {

    final static String TO_SHORT_FIELD_MESSAGE = "The min size of the field \"%s\" is \"%s\".";

    public TooShortFieldException(String field, int minSize) {
        super(TO_SHORT_FIELD_MESSAGE.formatted(field, minSize));
    }
}

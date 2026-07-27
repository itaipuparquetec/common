package br.org.itaipuparquetec.common.domain.exceptions;

public class TooShortFieldException extends RuntimeException {

    static final String TO_SHORT_FIELD_MESSAGE = "The min size of the field \"%s\" is \"%s\".";

    public TooShortFieldException(final String field, final int minSize) {
        super(TO_SHORT_FIELD_MESSAGE.formatted(field, minSize));
    }
}

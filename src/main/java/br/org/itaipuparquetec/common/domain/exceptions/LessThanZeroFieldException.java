package br.org.itaipuparquetec.common.domain.exceptions;

public class LessThanZeroFieldException extends RuntimeException {
    final static String LESS_THAN_ZERO_MESSAGE = "The field \"%s\" cannot be less than zero.";
    public LessThanZeroFieldException(String field) {
        super(LESS_THAN_ZERO_MESSAGE.formatted(field));
    }
}

package br.org.itaipuparquetec.common.domain.exceptions;

public class AlreadyExistsFieldsException extends RuntimeException {
    final static String ALREADY_EXITS_FIELDS_MESSAGE = "The fields \"%s\" already registered.";

    public AlreadyExistsFieldsException(String... field) {
        super(ALREADY_EXITS_FIELDS_MESSAGE.formatted(String.join(",", field)));
    }
}

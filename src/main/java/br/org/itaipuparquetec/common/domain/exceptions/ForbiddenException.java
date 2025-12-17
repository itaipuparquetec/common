package br.org.itaipuparquetec.common.domain.exceptions;

public class ForbiddenException extends RuntimeException {

    final static String ACCESS_DENIED_MESSAGE = "Access denied to resource \"%s\".";

    public ForbiddenException(String field) {super(ACCESS_DENIED_MESSAGE.formatted(field));}
}

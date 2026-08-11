package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions;

public class TenantConnectionException extends RuntimeException {

    public TenantConnectionException(final String message) {
        super(message);
    }

    public TenantConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

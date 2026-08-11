package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions;

public class TenantMigrationException extends RuntimeException {

    public TenantMigrationException(final String message) {
        super(message);
    }

    public TenantMigrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

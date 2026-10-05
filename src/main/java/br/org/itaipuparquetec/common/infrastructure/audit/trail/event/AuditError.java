package br.org.itaipuparquetec.common.infrastructure.audit.trail.event;

import br.org.itaipuparquetec.common.domain.exceptions.DomainException;

/**
 * Error data recorded in the audit trail. Only the type and message of a domain exception are kept, never the
 * cause chain. Technical errors carry no payload, since the original exception may contain sensitive data.
 */
public record AuditError(String type, String message) {

    private static final String TECHNICAL_ERROR_TYPE = "TechnicalException";

    public static AuditError technical() {
        return new AuditError(TECHNICAL_ERROR_TYPE, null);
    }

    public static AuditError business(final DomainException exception) {
        return new AuditError(exception.getClass().getSimpleName(), ((Throwable) exception).getMessage());
    }
}

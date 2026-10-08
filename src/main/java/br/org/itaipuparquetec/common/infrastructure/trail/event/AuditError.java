package br.org.itaipuparquetec.common.infrastructure.trail.event;

import br.org.itaipuparquetec.common.domain.exceptions.DomainException;
import br.org.itaipuparquetec.common.domain.exceptions.TechnicalException;

/**
 * Error data recorded in the audit trail: only the simple type name and the category. The exception message is
 * never recorded, since by the code rules it carries the offending value and may contain personal data; it stays
 * in the application log, correlated by the trace id.
 */
public record AuditError(String type, AuditErrorCategory category) {

    private static final String TECHNICAL_ERROR_TYPE = "TechnicalException";

    public static AuditError of(final Throwable failure) {
        if (failure instanceof TechnicalException) {
            return technical();
        }
        if (failure instanceof DomainException) {
            return new AuditError(failure.getClass().getSimpleName(), AuditErrorCategory.BUSINESS);
        }
        return technical();
    }

    private static AuditError technical() {
        return new AuditError(TECHNICAL_ERROR_TYPE, AuditErrorCategory.TECHNICAL);
    }
}

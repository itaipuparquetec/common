package br.org.itaipuparquetec.common.infrastructure.trail.event;

/**
 * Category of the failure that ended a use case execution.
 */
public enum AuditErrorCategory {
    BUSINESS(AuditResult.BUSINESS_ERROR),
    TECHNICAL(AuditResult.TECHNICAL_ERROR);

    private final AuditResult result;

    AuditErrorCategory(final AuditResult result) {
        this.result = result;
    }

    public AuditResult result() {
        return result;
    }
}

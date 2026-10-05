package br.org.itaipuparquetec.common.infrastructure.audit.trail.event;

/**
 * Outcome of a use case execution, as recorded in the audit trail.
 */
public enum AuditResult {
    SUCCESS,
    BUSINESS_ERROR,
    TECHNICAL_ERROR
}

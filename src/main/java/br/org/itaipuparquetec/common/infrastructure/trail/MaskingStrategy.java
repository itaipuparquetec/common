package br.org.itaipuparquetec.common.infrastructure.trail;

/**
 * How a classified value is rendered in the audit trail.
 */
public enum MaskingStrategy {
    /** Write the value in clear text. */
    NONE,
    /** Replace the value with a deterministic pseudonym (irreversible hash). */
    PSEUDONYMIZE,
    /** Replace the value with a fixed placeholder, discarding it entirely. */
    REDACT
}

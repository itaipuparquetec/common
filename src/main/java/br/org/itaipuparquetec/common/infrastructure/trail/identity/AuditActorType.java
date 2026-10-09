package br.org.itaipuparquetec.common.infrastructure.trail.identity;

/**
 * Nature of the actor that triggered a use case execution.
 */
public enum AuditActorType {
    /** User JWT. */
    USER,
    /** Client credentials JWT (the subject is the client id). */
    SERVICE,
    /** No JWT: schedulers, listeners and other internal flows. */
    SYSTEM
}

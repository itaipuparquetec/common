package br.org.itaipuparquetec.common.infrastructure.trail;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Decides whether a use case execution is recorded in the audit trail. Writes are always recorded; executions
 * inside a read-only transaction are recorded only when included by configuration or marked with
 * {@link AuditSensitiveRead}.
 */
public class AuditPolicy {

    private final boolean includeReadOnly;

    public AuditPolicy(final boolean includeReadOnly) {
        this.includeReadOnly = includeReadOnly;
    }

    public boolean skips(final Class<?> useCaseType) {
        return !includeReadOnly
                && TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                && !AnnotatedElementUtils.hasAnnotation(useCaseType, AuditSensitiveRead.class);
    }
}

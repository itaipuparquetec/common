package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantScope;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.RejectedExecutionException;

/**
 * Writes an outbox row in a transaction of its own, after the business transaction has ended. The write runs on a
 * deferred writer thread, because with the JPA transaction manager the connection of the business transaction is
 * only released after its completion callbacks: writing from the request thread would need a second connection
 * while still holding the first. When the write fails, or too many writes are waiting, the event is not lost: it
 * goes to the fallback sink (structured {@code AUDIT} log) and the failure is counted.
 */
public class AuditOutboxNewTransactionWriter {

    private final TransactionTemplate newTransaction;
    private final AuditOutboxStore store;
    private final AuditEventSink fallback;
    private final AuditMetrics metrics;
    private final DeferredWrites deferredWrites;
    private final AuditTenantScope tenantScope;

    public AuditOutboxNewTransactionWriter(final PlatformTransactionManager transactionManager,
                                           final AuditOutboxStore store, final AuditEventSink fallback,
                                           final AuditMetrics metrics, final DeferredWrites deferredWrites,
                                           final AuditTenantScope tenantScope) {
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.store = store;
        this.fallback = fallback;
        this.metrics = metrics;
        this.deferredWrites = deferredWrites;
        this.tenantScope = tenantScope;
    }

    /**
     * Writes the row once the transaction in progress completes, or right away when there is none.
     *
     * @param entry the row to write
     * @param event the event, kept for the log fallback
     */
    public void writeAfterCompletion(final AuditOutboxEntry entry, final AuditEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            write(entry, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new AfterCompletionOutboxWrite(this, entry, event));
    }

    /**
     * Hands the row to a deferred writer thread, which writes it with the tenant of the event in effect.
     *
     * @param entry the row to write
     * @param event the event, kept for the log fallback
     */
    public void writeLater(final AuditOutboxEntry entry, final AuditEvent event) {
        try {
            deferredWrites.submit(() -> tenantScope.runAs(event.tenant(), () -> write(entry, event)));
        } catch (final RejectedExecutionException saturated) {
            fallBackToLog(event);
        }
    }

    public void write(final AuditOutboxEntry entry, final AuditEvent event) {
        try {
            newTransaction.executeWithoutResult(status -> store.add(entry));
        } catch (final RuntimeException failure) {
            fallBackToLog(event);
        }
    }

    public void fallBackToLog(final AuditEvent event) {
        metrics.outboxWriteFailed();
        fallback.publish(event);
    }
}

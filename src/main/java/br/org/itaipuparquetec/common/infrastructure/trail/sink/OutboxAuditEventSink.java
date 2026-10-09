package br.org.itaipuparquetec.common.infrastructure.trail.sink;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditResult;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditEnvelopeSerializationException;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxEntry;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxEntryFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxNewTransactionWriter;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxStore;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Delivers audit events through the transactional outbox (ADR-031).
 *
 * <ul>
 *   <li>Success inside a writable transaction: the row joins the business transaction (atomic; if the insert
 *       fails the business operation fails too).</li>
 *   <li>Failure, read-only transaction or no transaction: the row is written in a transaction of its own after
 *       the business transaction ends, never holding two connections at once.</li>
 * </ul>
 */
public class OutboxAuditEventSink implements AuditEventSink {

    private final AuditOutboxEntryFactory entries;
    private final AuditOutboxStore store;
    private final AuditOutboxNewTransactionWriter newTransactionWriter;

    public OutboxAuditEventSink(final AuditOutboxEntryFactory entries, final AuditOutboxStore store,
                                final AuditOutboxNewTransactionWriter newTransactionWriter) {
        this.entries = entries;
        this.store = store;
        this.newTransactionWriter = newTransactionWriter;
    }

    @Override
    public void publish(final AuditEvent event) {
        final AuditOutboxEntry entry;
        try {
            entry = entries.entryOf(event);
        } catch (final AuditEnvelopeSerializationException failure) {
            newTransactionWriter.fallBackToLog(event);
            return;
        }
        if (joinsBusinessTransaction(event)) {
            store.add(entry);
            return;
        }
        newTransactionWriter.writeAfterCompletion(entry, event);
    }

    private static boolean joinsBusinessTransaction(final AuditEvent event) {
        return event.result() == AuditResult.SUCCESS
                && TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }
}

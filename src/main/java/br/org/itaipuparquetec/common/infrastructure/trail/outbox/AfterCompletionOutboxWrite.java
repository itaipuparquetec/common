package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Schedules the outbox row to be written once the business transaction has ended (committed or rolled back), so that
 * failure events survive a rollback.
 */
final class AfterCompletionOutboxWrite implements TransactionSynchronization {

    private final AuditOutboxNewTransactionWriter writer;
    private final AuditOutboxEntry entry;
    private final AuditEvent event;

    AfterCompletionOutboxWrite(final AuditOutboxNewTransactionWriter writer, final AuditOutboxEntry entry,
                               final AuditEvent event) {
        this.writer = writer;
        this.entry = entry;
        this.event = event;
    }

    @Override
    public void afterCompletion(final int status) {
        writer.writeLater(entry, event);
    }
}

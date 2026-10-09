package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

/**
 * Adds rows to the outbox table of the tenant database serving the current thread.
 */
public interface AuditOutboxStore {

    /**
     * Inserts the entry in the transaction in progress; the caller guarantees it is a writable transaction.
     *
     * @param entry the row to insert
     */
    void add(AuditOutboxEntry entry);
}

package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import jakarta.persistence.EntityManager;

/**
 * Inserts outbox rows through the JPA {@link EntityManager}, so the row travels in the same transaction (and on the
 * same tenant connection) as the business changes of the use case.
 */
public class JpaAuditOutboxStore implements AuditOutboxStore {

    private static final String INSERT = "INSERT INTO audit_outbox "
            + "(event_id, schema_version, tenant, source_service, trace_id, span_id, payload) "
            + "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)";

    private final EntityManager entityManager;

    public JpaAuditOutboxStore(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(final AuditOutboxEntry entry) {
        entityManager.createNativeQuery(INSERT)
                .setParameter(1, entry.eventId())
                .setParameter(2, entry.schemaVersion())
                .setParameter(3, entry.tenant())
                .setParameter(4, entry.sourceService())
                .setParameter(5, entry.traceId())
                .setParameter(6, entry.spanId())
                .setParameter(7, entry.payload())
                .executeUpdate();
    }
}

package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SQL access of the relay to the {@code audit_outbox} table of one tenant database.
 */
public class AuditOutboxTable {

    private static final int BACKLOG_CAP = 10_001;
    private static final String LOCK_OLDEST = "SELECT event_id, schema_version, tenant, source_service, trace_id, "
            + "span_id, payload, attempts FROM audit_outbox ORDER BY event_id LIMIT ? FOR UPDATE SKIP LOCKED";
    private static final String DELETE = "DELETE FROM audit_outbox WHERE event_id = ?";
    private static final String COUNT_ATTEMPT = "UPDATE audit_outbox SET attempts = attempts + 1 WHERE event_id = ?";
    private static final String BACKLOG = "SELECT count(*) FROM (SELECT 1 FROM audit_outbox LIMIT ?) AS capped";
    private static final String OLDEST_AGE = "SELECT COALESCE(EXTRACT(EPOCH FROM now() - "
            + "(SELECT created_at FROM audit_outbox ORDER BY event_id LIMIT 1))::bigint, 0)";

    private final JdbcTemplate jdbc;

    public AuditOutboxTable(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Locks, skipping rows already locked by other relay instances, the oldest rows of the outbox.
     *
     * @param limit maximum number of rows
     * @return the locked rows, oldest first
     */
    public List<AuditOutboxRow> lockOldest(final int limit) {
        return jdbc.query(LOCK_OLDEST, (rows, number) -> new AuditOutboxRow(
                rows.getObject("event_id", UUID.class),
                rows.getString("schema_version"),
                rows.getString("tenant"),
                rows.getString("source_service"),
                rows.getString("trace_id"),
                rows.getString("span_id"),
                rows.getString("payload"),
                rows.getInt("attempts")), limit);
    }

    public void delete(final List<UUID> eventIds) {
        jdbc.batchUpdate(DELETE, eventIds.stream().map(id -> new Object[]{id}).toList());
    }

    public void countFailedAttempt(final List<UUID> eventIds) {
        jdbc.batchUpdate(COUNT_ATTEMPT, eventIds.stream().map(id -> new Object[]{id}).toList());
    }

    /**
     * @return the number of pending rows, capped to keep the query cheap when the backlog is huge
     */
    public long backlog() {
        return Objects.requireNonNullElse(jdbc.queryForObject(BACKLOG, Long.class, BACKLOG_CAP), 0L);
    }

    public long oldestAgeSeconds() {
        return Objects.requireNonNullElse(jdbc.queryForObject(OLDEST_AGE, Long.class), 0L);
    }
}

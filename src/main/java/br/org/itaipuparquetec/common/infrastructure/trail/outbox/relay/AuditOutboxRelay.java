package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditProperties;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Publishes the pending audit events of every tenant database to Kafka (ADR-031). For each tenant, in one
 * transaction: lock the oldest rows with {@code FOR UPDATE SKIP LOCKED} (instances do not compete for rows), send
 * them, and delete only the rows the broker acknowledged. A crash between the acknowledgement and the delete
 * resends the same {@code eventId}, which the consumer deduplicates. Rows that fail stay in the table with their
 * attempts counted; the trail never discards an event at the origin.
 */
@Slf4j
public class AuditOutboxRelay {

    private final AuditOutboxCatalog catalog;
    private final KafkaAuditOutboxPublisher publisher;
    private final AuditMetrics metrics;
    private final AuditProperties.Relay settings;

    public AuditOutboxRelay(final AuditOutboxCatalog catalog, final KafkaAuditOutboxPublisher publisher,
                            final AuditMetrics metrics, final AuditProperties.Relay settings) {
        this.catalog = catalog;
        this.publisher = publisher;
        this.metrics = metrics;
        this.settings = settings;
    }

    public void relayAllTenants() {
        catalog.forEachTenant(this::relayTenantSafely);
    }

    private void relayTenantSafely(final String tenant) {
        try {
            relayTenant(tenant);
        } catch (final RuntimeException failure) {
            log.warn("Audit outbox relay failed for tenant {}; the rows stay pending", tenant, failure);
        }
    }

    private void relayTenant(final String tenant) {
        final var dataSource = catalog.dataSourceOf(tenant);
        final var table = new AuditOutboxTable(new JdbcTemplate(dataSource));
        new TransactionTemplate(new DataSourceTransactionManager(dataSource))
                .executeWithoutResult(status -> relayBatch(table));
        metrics.observeBacklog(tenant, table.backlog(), table.oldestAgeSeconds());
    }

    private void relayBatch(final AuditOutboxTable table) {
        final var deliveries = sendAll(table.lockOldest(settings.batchSize()));
        final var deadline = Instant.now().plus(settings.sendTimeout());
        final List<AuditOutboxRow> acknowledged = new ArrayList<>();
        final List<AuditOutboxRow> failed = new ArrayList<>();
        for (final var delivery : deliveries) {
            (delivery.acknowledgedBefore(deadline) ? acknowledged : failed).add(delivery.row());
        }
        table.delete(idsOf(acknowledged));
        table.countFailedAttempt(idsOf(failed));
        metrics.published(acknowledged.size());
        metrics.publishFailed(failed.size());
        failed.forEach(this::alertWhenStuck);
    }

    private List<PendingDelivery> sendAll(final List<AuditOutboxRow> rows) {
        final List<PendingDelivery> deliveries = new ArrayList<>();
        for (final var row : rows) {
            final var delivery = new PendingDelivery(row, publisher.publish(row));
            deliveries.add(delivery);
            if (delivery.brokerUnreachable()) {
                break;
            }
        }
        return deliveries;
    }

    private void alertWhenStuck(final AuditOutboxRow row) {
        if (row.attempts() + 1 >= settings.alertAttempts()) {
            log.error("Audit event {} of tenant {} could not be published after {} attempts",
                    row.eventId(), row.tenant(), row.attempts() + 1);
        }
    }

    private static List<UUID> idsOf(final List<AuditOutboxRow> rows) {
        return rows.stream().map(AuditOutboxRow::eventId).toList();
    }
}

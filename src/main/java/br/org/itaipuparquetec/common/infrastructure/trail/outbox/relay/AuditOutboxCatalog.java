package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import javax.sql.DataSource;
import java.util.function.Consumer;

/**
 * Tells the relay which databases hold an outbox.
 */
public interface AuditOutboxCatalog {

    void forEachTenant(Consumer<String> action);

    DataSource dataSourceOf(String tenant);
}

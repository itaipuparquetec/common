package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import javax.sql.DataSource;
import java.util.function.Consumer;

/**
 * Catalog of a service with a single database (no multitenancy).
 */
public class SingleDatabaseOutboxCatalog implements AuditOutboxCatalog {

    private final String tenant;
    private final DataSource dataSource;

    public SingleDatabaseOutboxCatalog(final String tenant, final DataSource dataSource) {
        this.tenant = tenant;
        this.dataSource = dataSource;
    }

    @Override
    public void forEachTenant(final Consumer<String> action) {
        action.accept(tenant);
    }

    @Override
    public DataSource dataSourceOf(final String tenantName) {
        return dataSource;
    }
}

package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Catalog of a database-per-tenant service. The tenants are discovered in the central database (the same source
 * the migration service uses) and refreshed periodically, so a tenant without traffic after a restart, or created
 * meanwhile, still has its pending rows published. When a refresh fails the last known list is kept.
 */
@Slf4j
public class MultitenantOutboxCatalog implements AuditOutboxCatalog {

    private final PostgreSQLMigrationServiceImpl tenantDiscovery;
    private final TenantDataSourceRegistryImpl dataSources;
    private final Clock clock;
    private final Duration refreshInterval;
    private Set<String> tenants = Set.of();
    private Instant refreshedAt = Instant.MIN;

    public MultitenantOutboxCatalog(final PostgreSQLMigrationServiceImpl tenantDiscovery,
                                    final TenantDataSourceRegistryImpl dataSources, final Clock clock,
                                    final Duration refreshInterval) {
        this.tenantDiscovery = tenantDiscovery;
        this.dataSources = dataSources;
        this.clock = clock;
        this.refreshInterval = refreshInterval;
    }

    @Override
    public void forEachTenant(final Consumer<String> action) {
        currentTenants().forEach(action);
    }

    @Override
    public DataSource dataSourceOf(final String tenant) {
        return dataSources.getDataSourceForTenant(tenant);
    }

    private synchronized Set<String> currentTenants() {
        final var now = clock.instant();
        if (now.isBefore(refreshedAt.plus(refreshInterval))) {
            return tenants;
        }
        refreshedAt = now;
        try {
            tenants = Set.copyOf(tenantDiscovery.listTenants());
        } catch (final RuntimeException failure) {
            log.warn("Could not refresh the tenants of the audit outbox relay; keeping the last known list", failure);
        }
        return tenants;
    }
}

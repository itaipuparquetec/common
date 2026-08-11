package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantConnectionException;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers the DataSource for each tenantId lazily (without overpopulating connections).
 */
@Slf4j
public class TenantDataSourceRegistryImpl {

    private final ConcurrentHashMap<String, HikariDataSource> poolsByTenant = new ConcurrentHashMap<>();
    private final TenantPoolFactory tenantPoolFactory;

    public TenantDataSourceRegistryImpl(final TenantPoolFactory tenantPoolFactory) {
        this.tenantPoolFactory = tenantPoolFactory;
    }

    public DataSource getDataSourceForTenant(final String tenantId) {
        return poolsByTenant.computeIfAbsent(tenantId, this::createRuntimePoolForTenant);
    }

    public Connection openConnectionForTenant(final String tenantId) {
        try {
            return getDataSourceForTenant(tenantId).getConnection();
        } catch (final SQLException e) {
            throw new TenantConnectionException(
                    "Cannot open connection for tenant '" + tenantId + "'", e);
        }
    }

    private HikariDataSource createRuntimePoolForTenant(final String tenantId) {
        log.info("Creating lazy datasource pool for tenant {}", tenantId);
        return tenantPoolFactory.buildRuntimePoolFor(tenantId);
    }

    @PreDestroy
    public void closeAllPools() {
        poolsByTenant.values().forEach(HikariDataSource::close);
        poolsByTenant.clear();
    }
}

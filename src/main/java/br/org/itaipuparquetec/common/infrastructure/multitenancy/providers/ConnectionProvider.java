package br.org.itaipuparquetec.common.infrastructure.multitenancy.providers;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Connection provider for multitenancy. Provides the tenant connection to hibernate.
 * Note that connections are lazy (without overpopulating connections).
 */
@RequiredArgsConstructor
public class ConnectionProvider implements MultiTenantConnectionProvider<String>, HibernatePropertiesCustomizer {

    private static final String HUBTI_TENANT = "hubti";

    private final transient TenantDataSourceRegistryImpl tenantDataSourceRegistryImpl;

    @Override
    public Connection getAnyConnection() {
        return tenantDataSourceRegistryImpl.openConnectionForTenant(HUBTI_TENANT);
    }

    @Override
    public void releaseAnyConnection(final Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(final String tenantId) {
        return tenantDataSourceRegistryImpl.openConnectionForTenant(tenantId);
    }

    @Override
    public void releaseConnection(final String tenantId, final Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public void customize(final Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(final Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> unwrapType) {
        return null;
    }
}

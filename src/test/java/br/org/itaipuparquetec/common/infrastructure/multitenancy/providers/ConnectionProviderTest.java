package br.org.itaipuparquetec.common.infrastructure.multitenancy.providers;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import org.hibernate.cfg.MultiTenancySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionProviderTest {

    private TenantDataSourceRegistryImpl registry;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() {
        registry = mock(TenantDataSourceRegistryImpl.class);
        connectionProvider = new ConnectionProvider(registry);
    }

    @Test
    void anyConnectionComesFromTheHubtiTenant() {
        final Connection connection = mock(Connection.class);
        when(registry.openConnectionForTenant("hubti")).thenReturn(connection);

        assertThat(connectionProvider.getAnyConnection()).isSameAs(connection);
    }

    @Test
    void connectionComesFromTheGivenTenant() {
        final Connection connection = mock(Connection.class);
        when(registry.openConnectionForTenant("tarcisio_tenant")).thenReturn(connection);

        assertThat(connectionProvider.getConnection("tarcisio_tenant")).isSameAs(connection);
    }

    @Test
    void releaseAnyConnectionClosesIt() throws Exception {
        final Connection connection = mock(Connection.class);

        connectionProvider.releaseAnyConnection(connection);

        verify(connection).close();
    }

    @Test
    void releaseConnectionClosesIt() throws Exception {
        final Connection connection = mock(Connection.class);

        connectionProvider.releaseConnection("tarcisio_tenant", connection);

        verify(connection).close();
    }

    @Test
    void customizeRegistersItselfAsTheMultiTenantConnectionProvider() {
        final Map<String, Object> properties = new HashMap<>();

        connectionProvider.customize(properties);

        assertThat(properties.get(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER)).isSameAs(connectionProvider);
    }

    @Test
    void doesNotSupportAggressiveReleaseAndIsNotUnwrappable() {
        assertThat(connectionProvider.supportsAggressiveRelease()).isFalse();
        assertThat(connectionProvider.isUnwrappableAs(String.class)).isFalse();
        assertThat(connectionProvider.unwrap(String.class)).isNull();
    }
}

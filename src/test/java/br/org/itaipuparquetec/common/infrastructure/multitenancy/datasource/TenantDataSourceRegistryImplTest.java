package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantConnectionException;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDataSourceRegistryImplTest {

    private FakeTenantPoolFactory poolFactory;
    private TenantDataSourceRegistryImpl registry;

    @BeforeEach
    void setUp() {
        poolFactory = new FakeTenantPoolFactory();
        registry = new TenantDataSourceRegistryImpl(poolFactory);
    }

    @AfterEach
    void tearDown() {
        registry.closeAllPools();
    }

    @Test
    void createsThePoolOnFirstAccessAndReusesItAfterwards() {
        final DataSource first = registry.getDataSourceForTenant("tarcisio_tenant");
        final DataSource second = registry.getDataSourceForTenant("tarcisio_tenant");

        assertThat(first).isSameAs(second);
        assertThat(poolFactory.buildCountFor("tarcisio_tenant")).isEqualTo(1);
    }

    @Test
    void keepsOnePoolPerTenant() {
        final DataSource tarcisio = registry.getDataSourceForTenant("tarcisio_tenant");
        final DataSource xxxxx = registry.getDataSourceForTenant("xxxxx_tenant");

        assertThat(tarcisio).isNotSameAs(xxxxx);
        assertThat(poolFactory.buildCountFor("tarcisio_tenant")).isEqualTo(1);
        assertThat(poolFactory.buildCountFor("xxxxx_tenant")).isEqualTo(1);
    }

    @Test
    void doesNotCreateAnyPoolUntilATenantIsAccessed() {
        assertThat(poolFactory.buildCountFor("tarcisio_tenant")).isZero();
    }

    @Test
    void openConnectionForReturnsAConnectionFromTheTenantPool() throws SQLException {
        final var pool = (HikariDataSource) registry.getDataSourceForTenant("tarcisio_tenant");
        final Connection connection = mock(Connection.class);
        when(pool.getConnection()).thenReturn(connection);

        assertThat(registry.openConnectionForTenant("tarcisio_tenant")).isSameAs(connection);
    }

    @Test
    void openConnectionForWrapsSqlExceptionInTenantConnectionException() throws SQLException {
        final var pool = (HikariDataSource) registry.getDataSourceForTenant("tarcisio_tenant");
        when(pool.getConnection()).thenThrow(new SQLException("boom"));

        assertThatThrownBy(() -> registry.openConnectionForTenant("tarcisio_tenant"))
                .isInstanceOf(TenantConnectionException.class)
                .hasMessageContaining("tarcisio_tenant");
    }

    @Test
    void closeAllPoolsClosesEveryCreatedPool() {
        registry.getDataSourceForTenant("tarcisio_tenant");
        registry.getDataSourceForTenant("xxxxx_tenant");

        registry.closeAllPools();

        verify(poolFactory.poolFor("tarcisio_tenant")).close();
        verify(poolFactory.poolFor("xxxxx_tenant")).close();
    }
}

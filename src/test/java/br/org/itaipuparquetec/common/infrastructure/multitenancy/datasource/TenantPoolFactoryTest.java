package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantPoolFactoryTest {

    private static final String CENTRAL_URL = "jdbc:postgresql://localhost:5432/hubti";
    private static final int MAX_POOL_SIZE = 5;

    private final TenantPoolFactory factory = new TenantPoolFactory(
            "org.postgresql.Driver", "common", "SELECT 1",
            MAX_POOL_SIZE, 0, 30000L,
            new TenantConnectionInfoProvider(CENTRAL_URL, "user", "pass"));

    private HikariDataSource pool;

    @AfterEach
    void closePool() {
        if (pool != null)
            pool.close();
    }

    @Test
    void runtimePoolUsesConfiguredSizeAndIsIdleReleasable() {
        pool = factory.buildRuntimePoolFor("tarcisio_tenant");

        assertThat(pool.getMaximumPoolSize()).isEqualTo(MAX_POOL_SIZE);
        assertThat(pool.getMinimumIdle()).isZero();
        assertThat(pool.getJdbcUrl()).endsWith("/tarcisio_tenant");
        assertThat(pool.getPoolName()).isEqualTo("HikariPool-tarcisio_tenant");
    }

    @Test
    void disposableMigrationPoolAllowsFlywaysTwoConcurrentConnections() {
        pool = factory.buildDisposableMigrationPoolFor("tarcisio_tenant");

        assertThat(pool.getMaximumPoolSize()).isEqualTo(2);
        assertThat(pool.getMinimumIdle()).isZero();
    }
}

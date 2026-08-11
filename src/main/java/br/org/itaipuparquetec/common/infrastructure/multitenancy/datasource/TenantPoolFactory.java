package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import com.zaxxer.hikari.HikariDataSource;

public class TenantPoolFactory {

    /**
     * Flyway holds the main connection and opens a separate migration connection at the same time.
     */
    private static final int MIGRATION_POOL_SIZE = 2;

    private final String driverClassName;
    private final String schema;
    private final String connectionInitSql;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long idleTimeout;
    private final TenantConnectionInfoProvider connectionInfoProvider;

    public TenantPoolFactory(final String driverClassName,
                             final String schema,
                             final String connectionInitSql,
                             final int maximumPoolSize,
                             final int minimumIdle,
                             final long idleTimeout,
                             final TenantConnectionInfoProvider connectionInfoProvider) {
        this.driverClassName = driverClassName;
        this.schema = schema;
        this.connectionInitSql = connectionInitSql;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.idleTimeout = idleTimeout;
        this.connectionInfoProvider = connectionInfoProvider;
    }

    public HikariDataSource buildRuntimePoolFor(final String tenantId) {
        final HikariDataSource pool = newPoolFor(tenantId, maximumPoolSize);
        pool.setMinimumIdle(minimumIdle);
        pool.setIdleTimeout(idleTimeout);
        return pool;
    }

    public HikariDataSource buildDisposableMigrationPoolFor(final String tenantId) {
        final HikariDataSource pool = newPoolFor(tenantId, MIGRATION_POOL_SIZE);
        pool.setMinimumIdle(0);
        return pool;
    }

    private HikariDataSource newPoolFor(final String tenantId, final int poolSize) {
        final TenantConnectionInfo info = connectionInfoProvider.resolveInfoFor(tenantId);
        final HikariDataSource pool = new HikariDataSource();
        pool.setPoolName("HikariPool-" + tenantId);
        pool.setJdbcUrl(info.jdbcUrl());
        pool.setUsername(info.username());
        pool.setPassword(info.password());
        pool.setSchema(schema);
        pool.setDriverClassName(driverClassName);
        pool.setMaximumPoolSize(poolSize);
        pool.setConnectionInitSql(connectionInitSql);
        return pool;
    }
}

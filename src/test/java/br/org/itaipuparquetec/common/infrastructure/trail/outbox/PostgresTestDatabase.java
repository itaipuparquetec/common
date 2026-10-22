package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration.AuditOutboxMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public final class PostgresTestDatabase {

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);
    private static final int STARTUP_ATTEMPTS = 3;
    private static final PostgreSQLContainer<?> POSTGRES = startedPostgres();

    private PostgresTestDatabase() {
    }

    public static String newSchemaName() {
        return "audit_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static HikariDataSource dataSourceOf(final String schema, final int maximumPoolSize) {
        createSchema(schema);
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(3000L);
        config.setConnectionInitSql("SET search_path TO " + schema + ", public");
        return new HikariDataSource(config);
    }

    public static HikariDataSource dataSourceWithOutboxTable(final String schema, final int maximumPoolSize) {
        try (HikariDataSource migrationPool = dataSourceOf(schema, 2)) {
            new AuditOutboxMigrator().migrate(migrationPool, schema);
        }
        return dataSourceOf(schema, maximumPoolSize);
    }

    private static PostgreSQLContainer<?> startedPostgres() {
        final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
                .waitingFor(reachableFromTheTests())
                .withStartupAttempts(STARTUP_ATTEMPTS);
        postgres.start();
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
        return postgres;
    }

    private static WaitAllStrategy reachableFromTheTests() {
        return new WaitAllStrategy()
                .withStrategy(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                .withStrategy(Wait.forListeningPort())
                .withStartupTimeout(STARTUP_TIMEOUT);
    }

    private static void createSchema(final String schema) {
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        } catch (final SQLException failure) {
            throw new IllegalStateException("Cannot create the test schema '" + schema + "'", failure);
        }
    }
}

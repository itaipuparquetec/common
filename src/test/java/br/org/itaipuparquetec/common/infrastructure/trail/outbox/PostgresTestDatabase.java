package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration.AuditOutboxMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;

public final class PostgresTestDatabase {

    private static final String DATABASE = "hubti";
    private static final int POSTGRES_PORT = 5433;
    private static final GenericContainer<?> POSTGRES = startedPostgres();

    private PostgresTestDatabase() {
    }

    public static String newSchemaName() {
        return "audit_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static HikariDataSource dataSourceOf(final String schema, final int maximumPoolSize) {
        createSchema(schema);
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl());
        config.setUsername(DATABASE);
        config.setPassword(DATABASE);
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

    private static GenericContainer<?> startedPostgres() {
        final GenericContainer<?> postgres = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withFileFromPath("Dockerfile", Paths.get("src/test/resources/postgres/Dockerfile")))
                .withNetworkAliases("localhost")
                .withEnv("POSTGRES_DB", DATABASE)
                .withEnv("POSTGRES_USER", DATABASE)
                .withEnv("POSTGRES_PASSWORD", DATABASE)
                .withExposedPorts(POSTGRES_PORT)
                .withNetwork(Network.SHARED)
                .withStartupTimeout(Duration.ofMinutes(2))
                .withNetworkMode("bridge")
                .withStartupAttempts(3);
        postgres.start();
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
        return postgres;
    }

    private static String jdbcUrl() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(POSTGRES_PORT) + "/" + DATABASE;
    }

    private static void createSchema(final String schema) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl(), DATABASE, DATABASE);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        } catch (final SQLException failure) {
            throw new IllegalStateException("Cannot create the test schema '" + schema + "'", failure);
        }
    }
}

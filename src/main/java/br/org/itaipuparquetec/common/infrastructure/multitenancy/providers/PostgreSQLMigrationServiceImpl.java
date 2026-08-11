package br.org.itaipuparquetec.common.infrastructure.multitenancy.providers;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantPoolFactory;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantMigrationException;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Service de migration resiliente para cada microservice.
 * As responsabilidades são:
 * - Criar a database se ela não existir;
 * - Criar a extensão unaccent caso ela não exista;
 * - Migrar o microservice para aquela database caso não tenha sido migrado (utilizando flyway);
 * - Fazer tudo isso para todos os tenants quando o microservices for startado, ou;
 * - Migrar um tenant específico quando solicitado (ex: via evento de criação de tenant).
 */
@Slf4j
@RequiredArgsConstructor
public class PostgreSQLMigrationServiceImpl {

    private static final String HUBTI_TENANT = "hubti";
    private static final String UNIQUE_VIOLATION_STATE = "23505";
    private static final String DUPLICATE_OBJECT_STATE = "42710";
    private static final int MAX_IDENTIFIER_LENGTH = 63;
    private static final Pattern VALID_TENANT_NAME = Pattern.compile("^[a-zA-Z_]\\w*$");

    private final TenantDataSourceRegistryImpl tenantDataSourceRegistryImpl;
    private final TenantPoolFactory tenantPoolFactory;

    @PostConstruct
    public void init() {
        migrateAllTenants();
    }

    public void migrateAllTenants() {
        getAllTenants().forEach(this::migrateTenant);
    }

    public void migrateTenant(final String tenantId) {
        log.info("Migrating name {}", tenantId);
        sanitizeTenantName(tenantId);
        createDatabaseIfNotExists(tenantId);
        ensureUnaccentExtension(tenantId);
        migrateFlyway(tenantId);
        log.info("Tenant {} migrated", tenantId);
    }

    /**
     * Guard the tenant name against SQL injection before it is concatenated into DDL statements
     * such as CREATE DATABASE, where PostgreSQL identifiers cannot be bound as parameters.
     * Only valid unquoted PostgreSQL identifiers are accepted; anything else is rejected.
     *
     * @param tenantName {@link String}
     */
    private void sanitizeTenantName(final String tenantName) {
        if (tenantName == null || !VALID_TENANT_NAME.matcher(tenantName).matches()) {
            throw new TenantMigrationException("Invalid tenant name '" + tenantName
                    + "'. Expected letters, digits and underscores, starting with a letter or underscore, matching "
                    + VALID_TENANT_NAME.pattern());
        }
        if (tenantName.length() > MAX_IDENTIFIER_LENGTH) {
            throw new TenantMigrationException("Invalid tenant name '" + tenantName + "'. Expected at most "
                    + MAX_IDENTIFIER_LENGTH + " characters, but got " + tenantName.length());
        }
    }

    /**
     * Execute the flyway migrations on a disposable single-connection pool that is
     * closed right after the migration, so tenants do not accumulate open connections.
     *
     * @param tenantId {@link String}
     */
    private void migrateFlyway(final String tenantId) {
        try (HikariDataSource migrationPool = tenantPoolFactory.buildDisposableMigrationPoolFor(tenantId)) {
            final Flyway flyway = new FluentConfiguration()
                    .dataSource(migrationPool).baselineOnMigrate(true)
                    .baselineVersion("0").sqlMigrationPrefix("")
                    .failOnMissingLocations(true).defaultSchema(migrationPool.getSchema())
                    .locations("db/migrations").load();
            flyway.migrate();
        } catch (final Exception e) {
            log.error("Error while migrating name {}", tenantId, e);
            throw new TenantMigrationException("Flyway migration failed for tenant '" + tenantId + "'", e);
        }
    }

    /**
     * The unaccent extension is database-scoped and every microservice migrates its own schema
     * inside the shared tenant database. PostgreSQL's CREATE EXTENSION IF NOT EXISTS is not
     * concurrency-safe, so a duplicate error from a concurrent creator is treated as success.
     *
     * @param tenantId {@link String}
     */
    private void ensureUnaccentExtension(final String tenantId) {
        try (Connection connection = tenantDataSourceRegistryImpl.openConnectionForTenant(tenantId);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA pg_catalog");
        } catch (final SQLException e) {
            if (isExtensionAlreadyCreated(e)) {
                log.info("Extension unaccent already created concurrently for tenant {}, skipping", tenantId);
                return;
            }
            throw new TenantMigrationException("Cannot ensure unaccent extension for tenant '" + tenantId + "'", e);
        }
    }

    private boolean isExtensionAlreadyCreated(final SQLException e) {
        final String sqlState = e.getSQLState();
        return UNIQUE_VIOLATION_STATE.equals(sqlState) || DUPLICATE_OBJECT_STATE.equals(sqlState);
    }

    private HashSet<String> getAllTenants() {
        final var tenants = new HashSet<String>();
        try (Connection connection = tenantDataSourceRegistryImpl.openConnectionForTenant(HUBTI_TENANT);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT datname FROM pg_database WHERE datname NOT IN ('postgres', 'template0', 'template1') AND datname LIKE '%tenant%'")) {
            while (resultSet.next()) {
                tenants.add(resultSet.getString("datname"));
            }
            tenants.add(HUBTI_TENANT);
        } catch (final Exception e) {
            log.error("Error to get all tenants", e);
            throw new TenantMigrationException("Cannot list tenants from central database", e);
        }
        return tenants;
    }

    /**
     * Create a new database from tenantId, if not exists.
     * Without using springdata instrumentation. It's working standalone.
     *
     * @param tenantId {@link String}
     */
    @SuppressWarnings("java:S2077")
    private void createDatabaseIfNotExists(final String tenantId) {
        try (Connection connection = tenantDataSourceRegistryImpl.openConnectionForTenant(HUBTI_TENANT);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + tenantId);
            log.info("Success to create database {}", tenantId);
        } catch (final Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("already exists")) {
                log.info("Database {} already exists. We cant create it", tenantId);
                return;
            }
            log.error("Error to create database {}", tenantId, e);
            throw new TenantMigrationException("Cannot create database for tenant '" + tenantId + "'", e);
        }
    }
}

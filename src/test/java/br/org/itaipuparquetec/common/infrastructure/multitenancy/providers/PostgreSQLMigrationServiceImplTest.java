package br.org.itaipuparquetec.common.infrastructure.multitenancy.providers;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantConnectionInfoProvider;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantPoolFactory;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantMigrationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class PostgreSQLMigrationServiceImplTest {

    private static final String SCHEMA = "common";
    private static final String CENTRAL_DB = "hubti";
    private static final String CONNECTION_INIT_SQL = "SET search_path TO " + SCHEMA + ", public";
    private static final GenericContainer<?> POSTGRES;

    static {

        POSTGRES =
                new GenericContainer<>(
                        new ImageFromDockerfile()
                                .withFileFromPath(
                                        "Dockerfile",
                                        Paths.get("src/test/resources/postgres/Dockerfile")
                                )
                ).withNetworkAliases("localhost")
                        .withEnv("POSTGRES_DB", CENTRAL_DB)
                        .withEnv("POSTGRES_USER", CENTRAL_DB)
                        .withEnv("POSTGRES_PASSWORD", CENTRAL_DB)
                        .withExposedPorts(5433)
                        .withNetworkAliases("localhost")
                        .withNetwork(Network.SHARED)
                        .withStartupTimeout(Duration.ofMinutes(2))
                        .withNetworkMode("bridge")
                        .withStartupAttempts(3);

        POSTGRES.start();

        System.setProperty(
                "spring.datasource.url",
                "jdbc:postgresql://"
                        + POSTGRES.getHost()
                        + ":"
                        + POSTGRES.getMappedPort(5433)
                        + "/hubti"
        );

        System.setProperty("spring.datasource.username", "hubti");
        System.setProperty("spring.datasource.password", "hubti");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        String jdbcUrl =
                "jdbc:postgresql://"
                        + POSTGRES.getHost()
                        + ":"
                        + POSTGRES.getMappedPort(5433)
                        + "/hubti";

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> "hubti");
        registry.add("spring.datasource.password", () -> "hubti");

        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        registry.add("spring.datasource.hikari.schema", () -> "groups");
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO groups, public");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "30000");
        registry.add("hubti.multitenancy.enabled", () -> "true");
    }


    private TenantDataSourceRegistryImpl registry;
    private PostgreSQLMigrationServiceImpl service;

    @BeforeEach
    void setUp() {
        final String centralUrl = "jdbc:postgresql://" + POSTGRES.getHost()
                + ":" + POSTGRES.getFirstMappedPort() + "/" + CENTRAL_DB;
        final var connectionInfoProvider = new TenantConnectionInfoProvider(centralUrl, CENTRAL_DB, CENTRAL_DB);
        final var poolFactory = new TenantPoolFactory("org.postgresql.Driver", SCHEMA, CONNECTION_INIT_SQL,
                5, 0,30_000L, connectionInfoProvider);
        registry = new TenantDataSourceRegistryImpl(poolFactory);
        service = new PostgreSQLMigrationServiceImpl(registry, poolFactory);
    }

    @AfterEach
    void tearDown() {
        registry.closeAllPools();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tenant; DROP DATABASE " + CENTRAL_DB,
            "tenant name",
            "tenant'--",
            "tenant\" OR 1=1",
            "tenant)",
            "1tenant",
            "tenant-name",
            "",
            "tenant/*comment*/",
            "tenant;--"
    })
    void shouldRejectTenantNameWithSqlInjectionCharacters(final String maliciousTenantName) {
        final var exception = assertThrows(TenantMigrationException.class,
                () -> service.migrateTenant(maliciousTenantName));

        assertTrue(exception.getMessage().contains("Invalid tenant name"));
    }

    @Test
    void shouldRejectNullTenantName() {
        final var exception = assertThrows(TenantMigrationException.class,
                () -> service.migrateTenant(null));

        assertTrue(exception.getMessage().contains("Invalid tenant name"));
    }

    @Test
    void shouldRejectTenantNameLongerThanMaxIdentifierLength() {
        final var tooLongTenantName = "a".repeat(64);

        final var exception = assertThrows(TenantMigrationException.class,
                () -> service.migrateTenant(tooLongTenantName));

        assertTrue(exception.getMessage().contains("at most 63 characters"));
    }

    @Test
    void shouldRejectMaliciousTenantNameAndNeverCreateDatabase() throws Exception {
        final String malicious = "evil; CREATE DATABASE injected_by_sql_injection; --";

        assertThrows(TenantMigrationException.class, () -> service.migrateTenant(malicious));

        assertFalse(databaseExists("injected_by_sql_injection"),
                "no database should have been created from an injected statement");
    }

    @Test
    void shouldCreateDatabaseInstallUnaccentAndRunFlywayForNewTenant() throws Exception {
        final String tenant = "tenant_common_new";

        service.migrateTenant(tenant);

        assertTrue(databaseExists(tenant), "tenant database should have been created");
        assertTrue(extensionExists(tenant, "unaccent"), "unaccent extension should have been installed");
        assertTrue(flywayMigrationCount(tenant) > 0, "flyway migrations should have been applied");
    }

    @Test
    void shouldBeIdempotentWhenMigratingTheSameTenantTwice() {
        final String tenant = "tenant_common_idempotent";

        service.migrateTenant(tenant);

        assertDoesNotThrow(() -> service.migrateTenant(tenant));
    }

    @Test
    void shouldMigrateCentralDatabaseWhenMigratingAllTenants() throws Exception {
        service.migrateTenant("tenant_common_for_listing");

        service.migrateAllTenants();

        assertTrue(databaseExists(CENTRAL_DB), "central database should exist");
        assertTrue(flywayMigrationCount(CENTRAL_DB) > 0, "central database should have been migrated");
    }

    @Test
    void shouldThrowWhenListingTenantsFails() {
        final var isolatedService = serviceWithCentralConnectionFailure("central database is down");

        final var exception = assertThrows(TenantMigrationException.class, isolatedService::migrateAllTenants);

        assertTrue(exception.getMessage().contains("Cannot list tenants"));
    }

    @Test
    void shouldThrowWhenCreateDatabaseFailsWithUnexpectedError() {
        final var isolatedService = serviceWithCentralConnectionFailure("connection refused");

        final var exception = assertThrows(TenantMigrationException.class,
                () -> isolatedService.migrateTenant("tenant_unexpected_error"));

        assertTrue(exception.getMessage().contains("Cannot create database"));
    }

    @Test
    void shouldThrowWhenCreateDatabaseFailsWithoutErrorMessage() {
        final var registryMock = mock(TenantDataSourceRegistryImpl.class);
        when(registryMock.openConnectionForTenant(CENTRAL_DB)).thenThrow(new RuntimeException());
        final var isolatedService = new PostgreSQLMigrationServiceImpl(registryMock, mock(TenantPoolFactory.class));

        final var exception = assertThrows(TenantMigrationException.class,
                () -> isolatedService.migrateTenant("tenant_null_message"));

        assertTrue(exception.getMessage().contains("Cannot create database"));
    }

    @Test
    void shouldThrowWhenUnaccentExtensionFailsWithUnrecognizedSqlState() throws Exception {
        final String tenant = "tenant_unaccent_fail";
        final Connection centralConnection = connectionExecutingSuccessfully();
        final Connection tenantConnection = connectionFailingExecuteUpdate(new SQLException("boom", "99999"));
        final var registryMock = mock(TenantDataSourceRegistryImpl.class);
        when(registryMock.openConnectionForTenant(CENTRAL_DB)).thenReturn(centralConnection);
        when(registryMock.openConnectionForTenant(tenant)).thenReturn(tenantConnection);
        final var isolatedService = new PostgreSQLMigrationServiceImpl(registryMock, mock(TenantPoolFactory.class));

        final var exception = assertThrows(TenantMigrationException.class,
                () -> isolatedService.migrateTenant(tenant));

        assertTrue(exception.getMessage().contains("Cannot ensure unaccent extension"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"23505", "42710"})
    void shouldSwallowUnaccentExtensionErrorWhenAlreadyCreatedConcurrently(final String alreadyCreatedSqlState) throws Exception {
        final String tenant = "tenant_unaccent_race";
        final Connection centralConnection = connectionExecutingSuccessfully();
        final Connection tenantConnection = connectionFailingExecuteUpdate(new SQLException("already exists", alreadyCreatedSqlState));
        final var registryMock = mock(TenantDataSourceRegistryImpl.class);
        final var poolFactoryMock = mock(TenantPoolFactory.class);
        when(registryMock.openConnectionForTenant(CENTRAL_DB)).thenReturn(centralConnection);
        when(registryMock.openConnectionForTenant(tenant)).thenReturn(tenantConnection);
        when(poolFactoryMock.buildDisposableMigrationPoolFor(tenant)).thenThrow(new RuntimeException("no migration pool"));
        final var isolatedService = new PostgreSQLMigrationServiceImpl(registryMock, poolFactoryMock);

        final var exception = assertThrows(TenantMigrationException.class,
                () -> isolatedService.migrateTenant(tenant));

        assertTrue(exception.getMessage().contains("Flyway migration failed"),
                "unaccent error should have been swallowed, letting the migration reach flyway");
    }

    @Test
    void shouldThrowWhenFlywayMigrationFails() throws Exception {
        final String tenant = "tenant_flyway_fail";
        final Connection connection = connectionExecutingSuccessfully();
        final var registryMock = mock(TenantDataSourceRegistryImpl.class);
        final var poolFactoryMock = mock(TenantPoolFactory.class);
        when(registryMock.openConnectionForTenant(anyString())).thenReturn(connection);
        when(poolFactoryMock.buildDisposableMigrationPoolFor(tenant)).thenThrow(new RuntimeException("no migration pool"));
        final var isolatedService = new PostgreSQLMigrationServiceImpl(registryMock, poolFactoryMock);

        final var exception = assertThrows(TenantMigrationException.class,
                () -> isolatedService.migrateTenant(tenant));

        assertTrue(exception.getMessage().contains("Flyway migration failed"));
    }

    private PostgreSQLMigrationServiceImpl serviceWithCentralConnectionFailure(final String errorMessage) {
        final var registryMock = mock(TenantDataSourceRegistryImpl.class);
        when(registryMock.openConnectionForTenant(CENTRAL_DB)).thenThrow(new RuntimeException(errorMessage));
        return new PostgreSQLMigrationServiceImpl(registryMock, mock(TenantPoolFactory.class));
    }

    private static Connection connectionExecutingSuccessfully() throws SQLException {
        final Statement statement = mock(Statement.class);
        when(statement.executeUpdate(anyString())).thenReturn(0);
        final Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        return connection;
    }

    private static Connection connectionFailingExecuteUpdate(final SQLException error) throws SQLException {
        final Statement statement = mock(Statement.class);
        when(statement.executeUpdate(anyString())).thenThrow(error);
        final Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        return connection;
    }

    private boolean databaseExists(final String databaseName) throws Exception {
        try (Connection connection = registry.openConnectionForTenant(CENTRAL_DB);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean extensionExists(final String tenant, final String extension) throws Exception {
        try (Connection connection = registry.openConnectionForTenant(tenant);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM pg_extension WHERE extname = ?")) {
            statement.setString(1, extension);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private long flywayMigrationCount(final String tenant) throws Exception {
        try (Connection connection = registry.openConnectionForTenant(tenant);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT count(*) FROM " + SCHEMA + ".flyway_schema_history WHERE success = true")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}

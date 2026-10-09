package br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration;

import org.flywaydb.core.api.configuration.FluentConfiguration;

import javax.sql.DataSource;

/**
 * Creates the {@code audit_outbox} table with the script distributed by the starter
 * ({@code classpath:db/audit-migrations}). It uses a Flyway history table of its own, so it never conflicts with the
 * versions or the validation of the migrations of the service.
 */
public class AuditOutboxMigrator {

    public static final String LOCATION = "classpath:db/audit-migrations";
    public static final String HISTORY_TABLE = "audit_flyway_history";

    /**
     * Migrates the schema of the given data source.
     *
     * @param dataSource the tenant (or single) database
     * @param schema     the schema of the service; {@code null} uses the schema of the connection
     */
    public void migrate(final DataSource dataSource, final String schema) {
        final var configuration = new FluentConfiguration()
                .dataSource(dataSource)
                .table(HISTORY_TABLE)
                .locations(LOCATION)
                .sqlMigrationPrefix("")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .failOnMissingLocations(true);
        if (schema != null) {
            configuration.defaultSchema(schema);
        }
        configuration.load().migrate();
    }
}

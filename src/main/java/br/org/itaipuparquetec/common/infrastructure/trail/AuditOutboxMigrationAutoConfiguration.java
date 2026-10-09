package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration.AuditOutboxMigrator;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration.AuditOutboxSchemaInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Creates the {@code audit_outbox} table in services with a single database. Services with database per tenant
 * (the Hubti APIs) get the table from the tenant migration service. Needs Flyway on the classpath; disable it with
 * {@code audit.outbox.migrate-schema=false} when the table is created by other means.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.flywaydb.core.Flyway")
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "audit", name = "sink", havingValue = "outbox")
@ConditionalOnProperty(prefix = "audit.outbox", name = "migrate-schema", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "hubti.multitenancy", name = "enabled", havingValue = "false", matchIfMissing = true)
public class AuditOutboxMigrationAutoConfiguration {

    @Bean
    public AuditOutboxSchemaInitializer auditOutboxSchemaInitializer(final DataSource dataSource) {
        return new AuditOutboxSchemaInitializer(new AuditOutboxMigrator(), dataSource);
    }
}

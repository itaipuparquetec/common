package br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration;

import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * Creates the outbox table at startup of a service with a single database. Services with database per tenant get it
 * from the tenant migration service instead.
 */
public class AuditOutboxSchemaInitializer implements InitializingBean {

    private final AuditOutboxMigrator migrator;
    private final DataSource dataSource;

    public AuditOutboxSchemaInitializer(final AuditOutboxMigrator migrator, final DataSource dataSource) {
        this.migrator = migrator;
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        migrator.migrate(dataSource, null);
    }
}

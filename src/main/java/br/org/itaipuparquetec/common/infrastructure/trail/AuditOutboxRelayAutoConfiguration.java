package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.SourceIdentity;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditOutboxCatalog;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditOutboxRelay;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditOutboxRelayLifecycle;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditProducerProperties;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.KafkaAuditOutboxPublisher;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.MultitenantOutboxCatalog;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.SingleDatabaseOutboxCatalog;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Wires the relay that publishes the outbox rows to Kafka. It runs in every instance of the service and only when
 * {@code audit.relay.enabled=true}. Its Kafka producer is dedicated: it never reuses the {@code KafkaTemplate} of
 * the service.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "audit", name = "sink", havingValue = "outbox")
@ConditionalOnProperty(prefix = "audit.relay", name = "enabled", havingValue = "true")
public class AuditOutboxRelayAutoConfiguration {

    @Bean
    public KafkaAuditOutboxPublisher auditOutboxKafkaPublisher(final KafkaProperties kafkaProperties,
                                                               final AuditProperties properties,
                                                               final SourceIdentity auditSourceIdentity) {
        final var clientId = "audit-outbox-relay-" + auditSourceIdentity.service();
        final var serviceProperties = kafkaProperties.buildProducerProperties(null);
        final var producer = new KafkaProducer<String, String>(
                AuditProducerProperties.from(serviceProperties, clientId));
        return new KafkaAuditOutboxPublisher(producer, properties.topics().events());
    }

    @Bean
    @ConditionalOnProperty(prefix = "hubti.multitenancy", name = "enabled", havingValue = "true")
    public AuditOutboxCatalog multitenantAuditOutboxCatalog(final PostgreSQLMigrationServiceImpl tenantDiscovery,
                                                            final TenantDataSourceRegistryImpl dataSources,
                                                            final AuditProperties properties) {
        return new MultitenantOutboxCatalog(tenantDiscovery, dataSources, Clock.systemUTC(),
                properties.relay().tenantRefresh());
    }

    @Bean
    @ConditionalOnProperty(prefix = "hubti.multitenancy", name = "enabled", havingValue = "false",
            matchIfMissing = true)
    public AuditOutboxCatalog singleDatabaseAuditOutboxCatalog(final DataSource dataSource) {
        return new SingleDatabaseOutboxCatalog(AuditTenantResolver.DEFAULT_TENANT, dataSource);
    }

    @Bean
    public AuditOutboxRelay auditOutboxRelay(final AuditOutboxCatalog catalog,
                                             final KafkaAuditOutboxPublisher auditOutboxKafkaPublisher,
                                             final AuditMetrics auditMetrics, final AuditProperties properties) {
        return new AuditOutboxRelay(catalog, auditOutboxKafkaPublisher, auditMetrics, properties.relay());
    }

    @Bean
    public AuditOutboxRelayLifecycle auditOutboxRelayLifecycle(final AuditOutboxRelay auditOutboxRelay,
                                                               final AuditProperties properties) {
        final var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("audit-outbox-relay-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return new AuditOutboxRelayLifecycle(scheduler, auditOutboxRelay::relayAllTenants,
                properties.relay().interval());
    }
}

package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.envelope.TrailEventEnvelopeFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantScope;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.SourceIdentity;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxEntryFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxNewTransactionWriter;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxStore;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.JpaAuditOutboxStore;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.ThreadPoolDeferredWrites;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.AuditMappers;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.OutboxAuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.Slf4jAuditEventSink;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wires the transactional outbox sink ({@code audit.enabled=true} and {@code audit.sink=outbox}, ADR-031). The row
 * is written through the application JPA transaction manager, on the connection of the tenant in effect. The
 * thread pool that writes the deferred rows is not an {@code Executor} bean, so it never makes Spring Boot back off
 * its own task executor.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "audit", name = "sink", havingValue = "outbox")
public class AuditOutboxAutoConfiguration {

    @Bean
    public AuditMetrics auditMetrics(final ObjectProvider<MeterRegistry> registry,
                                     final SourceIdentity auditSourceIdentity) {
        return new AuditMetrics(registry.getIfAvailable(SimpleMeterRegistry::new), auditSourceIdentity.service());
    }

    @Bean
    public AuditOutboxStore auditOutboxStore(final EntityManagerFactory entityManagerFactory) {
        return new JpaAuditOutboxStore(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
    }

    @Bean
    public ThreadPoolDeferredWrites auditDeferredWrites(final AuditProperties properties) {
        return new ThreadPoolDeferredWrites(properties.outbox().deferredWriterThreads(),
                properties.outbox().deferredQueueCapacity());
    }

    @Bean
    public AuditTenantScope auditTenantScope(final ObjectProvider<TenantIdentifierService> tenants) {
        final var available = tenants.getIfAvailable();
        return available == null ? AuditTenantScope.none() : AuditTenantScope.routingBy(available);
    }

    @Bean
    public AuditEventSink auditEventSink(final AuditProperties properties, final Pseudonymizer auditPseudonymizer,
                                         final AuditOutboxStore auditOutboxStore, final AuditMetrics auditMetrics,
                                         final PlatformTransactionManager transactionManager,
                                         final ThreadPoolDeferredWrites auditDeferredWrites,
                                         final AuditTenantScope auditTenantScope) {
        final var maskingMapper = AuditMappers.masking(properties, auditPseudonymizer);
        final var envelopes = new TrailEventEnvelopeFactory(maskingMapper, properties.maxInputBytes());
        final var entries = new AuditOutboxEntryFactory(envelopes, AuditMappers.envelope());
        final var fallback = new Slf4jAuditEventSink(maskingMapper);
        final var newTransactionWriter = new AuditOutboxNewTransactionWriter(
                transactionManager, auditOutboxStore, fallback, auditMetrics, auditDeferredWrites, auditTenantScope);
        return new OutboxAuditEventSink(entries, auditOutboxStore, newTransactionWriter);
    }
}

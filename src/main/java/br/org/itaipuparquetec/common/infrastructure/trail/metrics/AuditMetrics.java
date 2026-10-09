package br.org.itaipuparquetec.common.infrastructure.trail.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Operational metrics of the audit trail delivery (RNF 28.2): failures writing to the outbox, failures and
 * successes publishing to Kafka, and the backlog and age of the oldest pending event per tenant.
 */
public class AuditMetrics {

    private static final String SERVICE_TAG = "service";
    private static final String TENANT_TAG = "tenant";

    private final MeterRegistry registry;
    private final String service;
    private final Map<String, AtomicLong> backlogByTenant = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> oldestAgeByTenant = new ConcurrentHashMap<>();

    public AuditMetrics(final MeterRegistry registry, final String service) {
        this.registry = registry;
        this.service = service;
    }

    public void outboxWriteFailed() {
        counter("audit_outbox_write_failures").increment();
    }

    public void published(final int count) {
        counter("audit_outbox_published").increment(count);
    }

    public void publishFailed(final int count) {
        counter("audit_outbox_publish_failures").increment(count);
    }

    public void observeBacklog(final String tenant, final long backlog, final long oldestAgeSeconds) {
        gaugeValue(backlogByTenant, "audit_outbox_backlog", tenant).set(backlog);
        gaugeValue(oldestAgeByTenant, "audit_outbox_oldest_age_seconds", tenant).set(oldestAgeSeconds);
    }

    private Counter counter(final String name) {
        return registry.counter(name, SERVICE_TAG, service);
    }

    private AtomicLong gaugeValue(final Map<String, AtomicLong> values, final String name, final String tenant) {
        return values.computeIfAbsent(tenant, key -> registeredGauge(name, key));
    }

    private AtomicLong registeredGauge(final String name, final String tenant) {
        final var value = new AtomicLong();
        Gauge.builder(name, value, AtomicLong::get)
                .tags(Tags.of(SERVICE_TAG, service, TENANT_TAG, tenant))
                .register(registry);
        return value;
    }
}

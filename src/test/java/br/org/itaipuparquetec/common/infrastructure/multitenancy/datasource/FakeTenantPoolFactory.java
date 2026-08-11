package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import com.zaxxer.hikari.HikariDataSource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

class FakeTenantPoolFactory extends TenantPoolFactory {

    private final ConcurrentHashMap<String, AtomicInteger> buildsByTenant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HikariDataSource> poolsByTenant = new ConcurrentHashMap<>();

    FakeTenantPoolFactory() {
        super("org.postgresql.Driver", "common", "SELECT 1", 5, 0, 30000L,
                new TenantConnectionInfoProvider("jdbc:postgresql://localhost:5432/hubti", "user", "pass"));
    }

    @Override
    public HikariDataSource buildRuntimePoolFor(final String tenantId) {
        buildsByTenant.computeIfAbsent(tenantId, key -> new AtomicInteger()).incrementAndGet();
        final HikariDataSource pool = mock(HikariDataSource.class);
        poolsByTenant.put(tenantId, pool);
        return pool;
    }

    int buildCountFor(final String tenantId) {
        return buildsByTenant.getOrDefault(tenantId, new AtomicInteger()).get();
    }

    HikariDataSource poolFor(final String tenantId) {
        return poolsByTenant.get(tenantId);
    }
}

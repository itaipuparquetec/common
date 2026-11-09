package br.org.itaipuparquetec.common.infrastructure.tenancy;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The active tenants kept in memory (ADR-035): loaded once at startup and kept up to date by the events of the life of
 * the tenants, so validating a token never calls another service. The central tenant is always active: it cannot be
 * deactivated and its issuer gives the token that loads the others.
 */
public class InMemoryActiveTenantDirectory implements ActiveTenantDirectory {

    static final String CENTRAL_TENANT = "hubti";

    private final Set<String> activeTenants = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isActive(final String tenantName) {
        return CENTRAL_TENANT.equals(tenantName) || tenantName != null && activeTenants.contains(tenantName);
    }

    public void activate(final String tenantName) {
        if (tenantName != null && !tenantName.isBlank()) {
            activeTenants.add(tenantName);
        }
    }

    public void deactivate(final String tenantName) {
        if (tenantName != null) {
            activeTenants.remove(tenantName);
        }
    }
}

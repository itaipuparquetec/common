package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;

/**
 * Runs an action with a given tenant in effect on the current thread. Needed by the threads that write the deferred
 * outbox rows, which do not carry the tenant of the request that produced the event.
 */
public final class AuditTenantScope {

    private final TenantIdentifierService tenants;

    private AuditTenantScope(final TenantIdentifierService tenants) {
        this.tenants = tenants;
    }

    /**
     * @param tenants the service that routes the connections of the thread by tenant
     * @return a scope that switches the tenant of the thread while the action runs
     */
    public static AuditTenantScope routingBy(final TenantIdentifierService tenants) {
        return new AuditTenantScope(tenants);
    }

    /**
     * @return a scope for services with a single database: the action just runs
     */
    public static AuditTenantScope none() {
        return new AuditTenantScope(null);
    }

    public void runAs(final String tenant, final Runnable action) {
        if (tenants == null) {
            action.run();
            return;
        }
        tenants.setTenantId(tenant);
        try {
            action.run();
        } finally {
            tenants.clear();
        }
    }
}

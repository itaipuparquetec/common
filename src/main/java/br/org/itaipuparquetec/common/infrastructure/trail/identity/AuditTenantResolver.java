package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import java.util.function.Supplier;

/**
 * Resolves the effective tenant of the current execution (the tenant whose database serves the request).
 */
public class AuditTenantResolver {

    public static final String DEFAULT_TENANT = "hubti";

    private final Supplier<String> effectiveTenant;

    public AuditTenantResolver(final Supplier<String> effectiveTenant) {
        this.effectiveTenant = effectiveTenant;
    }

    public static AuditTenantResolver singleTenant() {
        return new AuditTenantResolver(() -> DEFAULT_TENANT);
    }

    public String current() {
        return effectiveTenant.get();
    }
}

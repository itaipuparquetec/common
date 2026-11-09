package br.org.itaipuparquetec.common.infrastructure.tenancy;

import java.util.function.Consumer;

/**
 * Where the active tenants are read once, at startup, to fill the {@link InMemoryActiveTenantDirectory}.
 */
@FunctionalInterface
public interface ActiveTenantsSource {

    void forEachActiveTenant(Consumer<String> tenantNameConsumer);
}

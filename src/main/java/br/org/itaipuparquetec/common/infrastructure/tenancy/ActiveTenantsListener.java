package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.kafka.annotation.KafkaListener;

/**
 * Keeps the {@link InMemoryActiveTenantDirectory} up to date with the events the TenantService publishes. Every
 * instance of every service needs every event, so each instance has a consumer group of its own, starting at the
 * latest event (the initial state comes from the {@link ActiveTenantsLoader}).
 */
public class ActiveTenantsListener {

    static final String TENANT_CREATED = "TENANT_CREATED_TOPIC";
    static final String TENANT_REACTIVATED = "TENANT_REACTIVATED_TOPIC";
    static final String TENANT_DEACTIVATED = "TENANT_DEACTIVATED_TOPIC";
    static final String TENANT_DELETED = "TENANT_DELETED_TOPIC";
    private static final String GROUP_OF_THE_INSTANCE = "${spring.application.name:hubti}-tenant-directory-${random.uuid}";
    private static final String ENABLED = "${hubti.auth.tenant-directory-listener.enabled:true}";
    private static final String FROM_THE_LATEST = "auto.offset.reset=latest";

    private final InMemoryActiveTenantDirectory directory;

    public ActiveTenantsListener(final InMemoryActiveTenantDirectory directory) {
        this.directory = directory;
    }

    @KafkaListener(topics = {TENANT_CREATED, TENANT_REACTIVATED}, groupId = GROUP_OF_THE_INSTANCE, autoStartup = ENABLED,
            properties = FROM_THE_LATEST)
    public void onTenantActivated(final String tenantName) {
        directory.activate(tenantName);
    }

    @KafkaListener(topics = {TENANT_DEACTIVATED, TENANT_DELETED}, groupId = GROUP_OF_THE_INSTANCE, autoStartup = ENABLED,
            properties = FROM_THE_LATEST)
    public void onTenantDeactivated(final String tenantName) {
        directory.deactivate(tenantName);
    }
}

package br.org.itaipuparquetec.common.infrastructure.multitenancy.events;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Receives the creation event of a new tenant to migrate it {@link ReceiveNewTenantCreatedEventImpl#handleNewTenantCreatedEvent}.
 * </p>
 * If an error occurs while migrating the newly created tenant, retries it in {@link ReceiveNewTenantCreatedEventImpl#handleNewTenantCreatedRetryEvent}.
 */
@Slf4j
@RequiredArgsConstructor
public class ReceiveNewTenantCreatedEventImpl {

    public static final String TENANT_CREATED_TOPIC = "TENANT_CREATED_TOPIC";
    public static final String TENANT_CREATED_RETRY_TOPIC = "TENANT_CREATED_RETRY_TOPIC";

    @Value("${spring.application.name}")
    private String applicationName;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PostgreSQLMigrationServiceImpl migrationService;

    /**
     * Try to handle the creation of a new tenant for migration.
     *
     * @param tenantId {@link String}
     */
    @KafkaListener(topics = TENANT_CREATED_TOPIC, autoStartup = "${hubti.multitenancy.enabled:false}")
    public void handleNewTenantCreatedEvent(final String tenantId) {
        try {
            migrationService.migrateTenant(tenantId);
            log.info("Tenant {} migrated to microservice {}", tenantId, applicationName);
        } catch (Exception e) {
            log.error("Tenant {} could not be migrated to microservice {}", tenantId, applicationName, e);
            kafkaTemplate.send(TENANT_CREATED_RETRY_TOPIC, tenantId);
        }
    }

    /**
     * Try to handle the creation of a new tenant for migration.
     *
     * @param tenantId {@link String}
     */
    @KafkaListener(topics = TENANT_CREATED_RETRY_TOPIC, autoStartup = "${hubti.multitenancy.enabled:false}")
    public void handleNewTenantCreatedRetryEvent(final String tenantId) {
        migrationService.migrateTenant(tenantId);
        log.info("RETRY: Tenant {} migrated to microservice {}", tenantId, applicationName);
    }
}

package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveTenantsListenerTest {

    private final InMemoryActiveTenantDirectory directory = new InMemoryActiveTenantDirectory();
    private final ActiveTenantsListener listener = new ActiveTenantsListener(directory);

    @Test
    void aCreatedOrReactivatedTenantIsActiveAndADeactivatedOrDeletedOneIsNot() {
        listener.onTenantActivated("acme_tenant");
        assertThat(directory.isActive("acme_tenant")).isTrue();

        listener.onTenantDeactivated("acme_tenant");
        assertThat(directory.isActive("acme_tenant")).isFalse();
    }

    @Test
    void everyInstanceListensToEveryEventFromTheLatestOne() throws NoSuchMethodException {
        final var activated = ActiveTenantsListener.class.getMethod("onTenantActivated", String.class).getAnnotation(KafkaListener.class);
        final var deactivated = ActiveTenantsListener.class.getMethod("onTenantDeactivated", String.class).getAnnotation(KafkaListener.class);

        assertThat(activated.topics()).containsExactly("TENANT_CREATED_TOPIC", "TENANT_REACTIVATED_TOPIC");
        assertThat(deactivated.topics()).containsExactly("TENANT_DEACTIVATED_TOPIC", "TENANT_DELETED_TOPIC");
        assertThat(activated.groupId()).contains("${random.uuid}");
        assertThat(deactivated.properties()).containsExactly("auto.offset.reset=latest");
    }
}

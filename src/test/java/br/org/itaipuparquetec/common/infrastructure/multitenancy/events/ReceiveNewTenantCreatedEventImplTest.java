package br.org.itaipuparquetec.common.infrastructure.multitenancy.events;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ReceiveNewTenantCreatedEventImplTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final PostgreSQLMigrationServiceImpl migrationService = mock(PostgreSQLMigrationServiceImpl.class);
    private ReceiveNewTenantCreatedEventImpl listener;

    @BeforeEach
    void setUp() {
        listener = new ReceiveNewTenantCreatedEventImpl(kafkaTemplate, migrationService);
    }

    @Test
    void migratesTheTenantAndDoesNotRetryOnSuccess() {
        listener.handleNewTenantCreatedEvent("tarcisio_tenant");

        verify(migrationService).migrateTenant("tarcisio_tenant");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishesToRetryTopicWhenMigrationFails() {
        doThrow(new RuntimeException("boom")).when(migrationService).migrateTenant("tarcisio_tenant");

        listener.handleNewTenantCreatedEvent("tarcisio_tenant");

        verify(kafkaTemplate).send(ReceiveNewTenantCreatedEventImpl.TENANT_CREATED_RETRY_TOPIC, "tarcisio_tenant");
    }

    @Test
    void retryListenerMigratesTheTenant() {
        listener.handleNewTenantCreatedRetryEvent("tarcisio_tenant");

        verify(migrationService).migrateTenant("tarcisio_tenant");
    }
}

package br.org.itaipuparquetec.common.infrastructure.async;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.TenantIdentifierServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantPropagationTaskDecoratorTest {

    private static final String ROOT_TENANT = "hubti";
    private static final String TENANT_OF_CALLER = "acme_tenant";

    private TenantIdentifierServiceImpl tenantIdentifierServiceImpl;
    private TenantPropagationTaskDecorator tenantPropagationTaskDecorator;

    @BeforeEach
    void setUp() {
        tenantIdentifierServiceImpl = new TenantIdentifierServiceImpl();
        tenantPropagationTaskDecorator = new TenantPropagationTaskDecorator(tenantIdentifierServiceImpl);
        tenantIdentifierServiceImpl.setTenantId(TENANT_OF_CALLER);
    }

    @AfterEach
    void tearDown() {
        tenantIdentifierServiceImpl.clear();
    }

    @Test
    void shouldRunTheTaskInTheTenantOfTheCallerOnAnotherThread() throws InterruptedException {
        final var tenantInTask = new AtomicReference<String>();
        final var decoratedTask = tenantPropagationTaskDecorator.decorate(
                () -> tenantInTask.set(tenantIdentifierServiceImpl.resolveCurrentTenantIdentifier()));

        runOnAnotherThread(decoratedTask);

        assertThat(tenantInTask).hasValue(TENANT_OF_CALLER);
    }

    @Test
    void shouldClearTheTenantOfTheThreadAfterTheTask() throws InterruptedException {
        final var tenantAfterTask = new AtomicReference<String>();
        final var decoratedTask = tenantPropagationTaskDecorator.decorate(() -> { });

        runOnAnotherThread(() -> {
            decoratedTask.run();
            tenantAfterTask.set(tenantIdentifierServiceImpl.resolveCurrentTenantIdentifier());
        });

        assertThat(tenantAfterTask).hasValue(ROOT_TENANT);
    }

    @Test
    void shouldClearTheTenantOfTheThreadWhenTheTaskFails() {
        final var decoratedTask = tenantPropagationTaskDecorator.decorate(() -> {
            throw new IllegalStateException("task failed");
        });

        assertThatThrownBy(decoratedTask::run).isInstanceOf(IllegalStateException.class);
        assertThat(tenantIdentifierServiceImpl.resolveCurrentTenantIdentifier()).isEqualTo(ROOT_TENANT);
    }

    private void runOnAnotherThread(final Runnable runnable) throws InterruptedException {
        final var thread = new Thread(runnable);
        thread.start();
        thread.join();
    }
}

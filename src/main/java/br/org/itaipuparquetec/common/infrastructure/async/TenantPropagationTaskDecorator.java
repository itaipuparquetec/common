package br.org.itaipuparquetec.common.infrastructure.async;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class TenantPropagationTaskDecorator implements TaskDecorator {

    private final TenantIdentifierService tenantIdentifierService;

    @Override
    public Runnable decorate(final Runnable runnable) {
        final var tenantOfCaller = tenantIdentifierService.resolveCurrentTenantIdentifier();
        return () -> runInTenant(tenantOfCaller, runnable);
    }

    private void runInTenant(final String tenantOfCaller, final Runnable runnable) {
        tenantIdentifierService.setTenantId(tenantOfCaller);
        try {
            runnable.run();
        } finally {
            tenantIdentifierService.clear();
        }
    }
}

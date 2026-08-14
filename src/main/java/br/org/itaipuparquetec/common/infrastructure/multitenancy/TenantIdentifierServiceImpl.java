package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import br.org.itaipuparquetec.common.domain.exceptions.ExceptionBuilder;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.Map;

public class TenantIdentifierServiceImpl implements TenantIdentifierService, CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    private static final String HUBTI_TENANT = "hubti";

    private final ThreadLocal<String> tenantId = new ThreadLocal<>();

    public void setTenantId(final String tenantName) {
        validateTenantName(tenantName);
        tenantId.set(tenantName);
    }

    private static void validateTenantName(final String tenantName){
        new ExceptionBuilder().whenNullOrEmpty(tenantName, "tenantName").thenThrows();
    }

    public void clear() {
        tenantId.remove();
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        final String id = tenantId.get();
        return id == null ? HUBTI_TENANT : id;
    }

    @Override
    public void customize(final Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(final String tenant) {
        return HUBTI_TENANT.equals(tenant);
    }
}

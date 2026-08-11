package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;

/**
 * DataSource router.
 * Routes according to the tenant resolved by {@link TenantIdentifierServiceImpl}.
 * Sets hubti as the default in postConstruct.
 * </p>
 * Depends on the PostgreSQLMigrationService bean, which runs the migrations when the system starts.
 */
@Slf4j
@RequiredArgsConstructor
public class RoutingDataSourceService extends AbstractRoutingDataSource {

    private static final String HUBTI_TENANT = "hubti";

    private final TenantIdentifierServiceImpl tenantIdentifierService;
    private final TenantDataSourceRegistryImpl tenantDataSourceRegistry;

    @PostConstruct
    public void postConstruct() {
        setTargetDataSources(new HashMap<>());
        setDefaultTargetDataSource(tenantDataSourceRegistry.getDataSourceForTenant(HUBTI_TENANT));
        initialize();
    }

    @Override
    protected @NonNull DataSource determineTargetDataSource() {
        return tenantDataSourceRegistry.getDataSourceForTenant(determineCurrentLookupKey());
    }

    @Override
    protected String determineCurrentLookupKey() {
        return tenantIdentifierService.resolveCurrentTenantIdentifier();
    }
}

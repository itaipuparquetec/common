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
 * Roteador de DataSources.
 * Faz o roteamento conforme o tenant resolvido por {@link TenantIdentifierServiceImpl}.
 * Seta o hubti como default no postConstruct.
 * </p>
 * Depende do bean PostgreSQLMigrationService, que realiza as migrations quando o sistema starta.
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

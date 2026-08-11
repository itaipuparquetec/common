package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantConnectionException;

/**
 * Constrói o TenantConnectionInfo para o tenantId informado.
 */
public class TenantConnectionInfoProvider {

    private final String centralUrl;
    private final String username;
    private final String password;

    public TenantConnectionInfoProvider(final String centralUrl, final String username, final String password) {
        this.centralUrl = centralUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Resolve o TenantConnectionInfo para o tenantId informado.
     * Por enquanto funciona com uma única base de dados.
     *
     * @param tenantId {@link String} tenantId informado.
     * @return {@link TenantConnectionInfo} TenantConnectionInfo construído/resolviod.
     */
    public TenantConnectionInfo resolveInfoFor(final String tenantId) {
        return new TenantConnectionInfo(buildJdbcUrlFor(tenantId), username, password);
    }

    private String buildJdbcUrlFor(final String tenantId) {
        final int lastSlash = centralUrl.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new TenantConnectionException(
                    "Central datasource url '" + centralUrl + "' must contain a database segment after '/'");
        }
        return centralUrl.substring(0, lastSlash + 1) + tenantId;
    }
}

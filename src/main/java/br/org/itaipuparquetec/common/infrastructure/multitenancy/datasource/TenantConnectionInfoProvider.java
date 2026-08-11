package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantConnectionException;

/**
 * Builds the TenantConnectionInfo for the given tenantId.
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
     * Resolves the TenantConnectionInfo for the given tenantId.
     * For now it works with a single database.
     *
     * @param tenantId {@link String} the given tenantId.
     * @return {@link TenantConnectionInfo} the built/resolved TenantConnectionInfo.
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

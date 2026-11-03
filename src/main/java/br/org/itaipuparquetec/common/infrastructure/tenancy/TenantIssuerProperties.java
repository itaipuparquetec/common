package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code hubti.auth.*}: the base of the tenant issuers ({@code <base>/<tenant>}), the address of the TenantService
 * and the credential the microservice uses in every tenant (client_credentials).
 */
@ConfigurationProperties(prefix = "hubti.auth")
public record TenantIssuerProperties(String issuerBaseUri,
                                     String tenantsUrl,
                                     String serviceClientId,
                                     String serviceClientSecret,
                                     Long tokenSafetyMarginSeconds) {

    private static final long DEFAULT_SAFETY_MARGIN_SECONDS = 30;

    public TenantIssuerProperties {
        tokenSafetyMarginSeconds = tokenSafetyMarginSeconds == null ? DEFAULT_SAFETY_MARGIN_SECONDS : tokenSafetyMarginSeconds;
    }
}

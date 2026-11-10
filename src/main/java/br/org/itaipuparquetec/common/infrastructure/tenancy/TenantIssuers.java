package br.org.itaipuparquetec.common.infrastructure.tenancy;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Each tenant has its own issuer, {@code <base>/<tenant name>} (ADR-036): a token is only valid in the tenant that
 * issued it. Translates a tenant to its issuer and an issuer back to its tenant.
 */
public class TenantIssuers {

    private static final Pattern TENANT_NAME = Pattern.compile("^[a-zA-Z_]\\w{0,62}$");

    private final String baseUri;

    public TenantIssuers(final String baseUri) {
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalArgumentException("Expected the base URI of the tenant issuers (hubti.auth.issuer-base-uri), but got '" + baseUri + "'");
        }
        this.baseUri = baseUri.replaceAll("/+$", "");
    }

    public String issuerOf(final String tenantName) {
        return baseUri + "/" + tenantName;
    }

    public Optional<String> tenantOf(final String issuer) {
        final var prefix = baseUri + "/";
        if (issuer == null || !issuer.startsWith(prefix)) {
            return Optional.empty();
        }
        final var tenantName = issuer.substring(prefix.length());
        return TENANT_NAME.matcher(tenantName).matches() ? Optional.of(tenantName) : Optional.empty();
    }
}

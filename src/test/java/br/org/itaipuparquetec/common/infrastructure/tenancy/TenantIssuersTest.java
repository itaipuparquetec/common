package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantIssuersTest {

    private final TenantIssuers tenantIssuers = new TenantIssuers("https://hubti.example.org/");

    @Test
    void buildsTheIssuerOfATenantUnderTheBaseWithoutTheTrailingSlash() {
        assertThat(tenantIssuers.issuerOf("acme_tenant")).isEqualTo("https://hubti.example.org/acme_tenant");
    }

    @ParameterizedTest
    @CsvSource({
            "https://hubti.example.org, https://hubti.example.org/acme_tenant",
            "https://hubti.example.org///, https://hubti.example.org/acme_tenant",
            "///, /acme_tenant"
    })
    void dropsEveryTrailingSlashOfTheBase(final String baseUri, final String issuer) {
        assertThat(new TenantIssuers(baseUri).issuerOf("acme_tenant")).isEqualTo(issuer);
    }

    @Test
    void readsTheTenantBackFromItsIssuer() {
        assertThat(tenantIssuers.tenantOf("https://hubti.example.org/acme_tenant")).contains("acme_tenant");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://other.example.org/acme_tenant",
            "https://hubti.example.org",
            "https://hubti.example.org/",
            "https://hubti.example.org/acme/tenant",
            "https://hubti.example.org/1acme",
            "https://hubti.example.org/acme-tenant"
    })
    void refusesAnIssuerThatIsNotTheIssuerOfATenant(final String issuer) {
        assertThat(tenantIssuers.tenantOf(issuer)).isEmpty();
    }

    @Test
    void refusesANullIssuer() {
        assertThat(tenantIssuers.tenantOf(null)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({"'   '"})
    void requiresTheBaseUri(final String baseUri) {
        assertThatThrownBy(() -> new TenantIssuers(baseUri))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hubti.auth.issuer-base-uri");
    }
}

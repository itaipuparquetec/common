package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteTenantJwtDecoderFactoryTest {

    private static final String ISSUER = "https://hubti.example.org/acme_tenant";

    @Test
    void buildsADecoderOfTheKeysOfTheIssuer() {
        assertThat(new RemoteTenantJwtDecoderFactory().decoderOf("acme_tenant", ISSUER)).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void acceptsATokenOfTheIssuerWithTheTenantOfTheIssuer() {
        final var result = RemoteTenantJwtDecoderFactory.validatorOf("acme_tenant", ISSUER).validate(tokenOf(ISSUER, "acme_tenant"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void refusesATokenWhoseTenantIsNotTheTenantOfTheIssuer() {
        final var result = RemoteTenantJwtDecoderFactory.validatorOf("acme_tenant", ISSUER).validate(tokenOf(ISSUER, "hubti"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void refusesATokenOfAnotherIssuer() {
        final var result = RemoteTenantJwtDecoderFactory.validatorOf("acme_tenant", ISSUER)
                .validate(tokenOf("https://hubti.example.org/hubti", "acme_tenant"));

        assertThat(result.hasErrors()).isTrue();
    }

    private static Jwt tokenOf(final String issuer, final String tenantName) {
        return Jwt.withTokenValue("token").header("alg", "RS256")
                .issuer(issuer).claim("tenant_name", tenantName)
                .issuedAt(Instant.now().minusSeconds(10)).expiresAt(Instant.now().plusSeconds(600))
                .build();
    }
}

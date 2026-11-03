package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TenantServiceTokenProviderTest {

    private static final String ACME_TOKEN_URI = "https://sentry.example.org/acme_tenant/oauth2/token";

    private MockRestServiceServer issuer;
    private TenantServiceTokenProvider provider;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        issuer = MockRestServiceServer.bindTo(builder).build();
        provider = new TenantServiceTokenProvider(new TenantIssuers("https://sentry.example.org"), builder.build(),
                "hubti-service", "secret", Duration.ofSeconds(30),
                Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void asksTheTokenToTheIssuerOfTheTenantWithTheServiceCredential() {
        issuer.expect(once(), requestTo(ACME_TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "grant_type", "client_credentials", "client_id", "hubti-service", "client_secret", "secret")))
                .andRespond(tokenResponse("acme-token", 3600));

        assertThat(provider.tokenOf("acme_tenant")).isEqualTo("acme-token");
        issuer.verify();
    }

    @Test
    void reusesTheTokenWhileItIsValid() {
        issuer.expect(once(), requestTo(ACME_TOKEN_URI)).andRespond(tokenResponse("acme-token", 3600));

        provider.tokenOf("acme_tenant");

        assertThat(provider.tokenOf("acme_tenant")).isEqualTo("acme-token");
        issuer.verify();
    }

    @Test
    void asksANewTokenWhenItExpiresWithinTheSafetyMargin() {
        issuer.expect(twice(), requestTo(ACME_TOKEN_URI)).andRespond(tokenResponse("short-token", 10));

        provider.tokenOf("acme_tenant");
        provider.tokenOf("acme_tenant");

        issuer.verify();
    }

    @Test
    void asksANewTokenAfterItIsInvalidated() {
        issuer.expect(twice(), requestTo(ACME_TOKEN_URI)).andRespond(tokenResponse("acme-token", 3600));

        provider.tokenOf("acme_tenant");
        provider.invalidate("acme_tenant");
        provider.tokenOf("acme_tenant");

        issuer.verify();
    }

    @Test
    void failsWhenTheIssuerAnswersWithoutAToken() {
        issuer.expect(requestTo(ACME_TOKEN_URI)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.tokenOf("acme_tenant"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("acme_tenant");
    }

    @Test
    void failsWhenTheIssuerAnswersWithAnEmptyBody() {
        issuer.expect(requestTo(ACME_TOKEN_URI)).andRespond(withSuccess());

        assertThatThrownBy(() -> provider.tokenOf("acme_tenant")).isInstanceOf(IllegalStateException.class);
    }

    private static org.springframework.test.web.client.ResponseCreator tokenResponse(final String token, final long expiresIn) {
        return withSuccess("{\"access_token\":\"" + token + "\",\"expires_in\":" + expiresIn + ",\"token_type\":\"Bearer\"}",
                MediaType.APPLICATION_JSON);
    }
}

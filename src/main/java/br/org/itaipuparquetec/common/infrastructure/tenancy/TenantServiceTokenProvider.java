package br.org.itaipuparquetec.common.infrastructure.tenancy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The access token a microservice uses to call another one in a tenant (client_credentials, RSF 8.9.3.3): asked to
 * the issuer of that tenant, since a token is only valid in the tenant that issued it (ADR-035). One token per tenant,
 * reused until a safety margin before it expires.
 */
public class TenantServiceTokenProvider {

    private final TenantIssuers tenantIssuers;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final Duration safetyMargin;
    private final Clock clock;
    private final Map<String, IssuedToken> tokensByTenant = new ConcurrentHashMap<>();

    public TenantServiceTokenProvider(final TenantIssuers tenantIssuers, final RestClient restClient,
                                      final String clientId, final String clientSecret,
                                      final Duration safetyMargin, final Clock clock) {
        this.tenantIssuers = tenantIssuers;
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.safetyMargin = safetyMargin;
        this.clock = clock;
    }

    public String tokenOf(final String tenantName) {
        final var cached = tokensByTenant.get(tenantName);
        if (cached != null && cached.isValidAt(clock.instant().plus(safetyMargin))) {
            return cached.value();
        }
        final var issued = requestTokenOf(tenantName);
        tokensByTenant.put(tenantName, issued);
        return issued.value();
    }

    public void invalidate(final String tenantName) {
        tokensByTenant.remove(tenantName);
    }

    private IssuedToken requestTokenOf(final String tenantName) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        final var response = restClient.post()
                .uri(tenantIssuers.issuerOf(tenantName) + "/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Expected an access token from the issuer of tenant '" + tenantName + "', but got none");
        }
        return new IssuedToken(response.accessToken(), clock.instant().plusSeconds(response.expiresIn()));
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }

    private record IssuedToken(String value, Instant expiresAt) {

        boolean isValidAt(final Instant instant) {
            return instant.isBefore(expiresAt);
        }
    }
}

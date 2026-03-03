package br.org.itaipuparquetec.common.infrastructure.keycloak;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class KeycloakTokenProvider {

    private static final String TOKEN_ENDPOINT = "/realms/{realm}/protocol/openid-connect/token";
    private static final String GRANT_TYPE = "client_credentials";

    private final WebClient keycloakRawWebClient;
    private final KeycloakProperties props;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    String getOrRefresh() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(props.tokenSafetyMarginSeconds()))) {
            return cachedToken;
        }
        return refresh();
    }

    private synchronized String refresh() {
        final KeycloakTokenResponse tokenResponse = keycloakRawWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(TOKEN_ENDPOINT)
                        .build(props.realm()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", GRANT_TYPE)
                            .with("client_id", props.clientId())
                            .with("client_secret", props.clientSecret())
                )
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.access_token() == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Error getting token from keycloak");
        }

        final int expiresIn = tokenResponse.expires_in();

        this.cachedToken = tokenResponse.access_token();
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
        return this.cachedToken;
    }

    void invalidate() {
        this.cachedToken = null;
        this.expiresAt = Instant.EPOCH;
    }
}
